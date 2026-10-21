package io.github.smokahs.culinarycompat.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import io.github.smokahs.culinarycompat.CulinaryCompat;
import vectorwing.farmersdelight.common.block.CookingPotBlock;
import vectorwing.farmersdelight.common.block.CuttingBoardBlock;
import vectorwing.farmersdelight.common.block.SkilletBlock;

@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID, value = Dist.CLIENT)
public final class ModelOffsets {
	static final ModelProperty<Boolean> OFFSET_APPLIES = new ModelProperty<>();

	private static final String CFB_NAMESPACE = "cookingforblockheads";
	private static final ResourceLocation CFB_COOKING_TABLE = ResourceLocation.fromNamespaceAndPath(CFB_NAMESPACE,
			"cooking_table");
	private static final int VERTEX_STRIDE_INTS = 8;

	// fd blocks + custom bakeware offsets
	private static final Rule CFB_BELOW_DOWN = new Rule(-1.0f / 16.0f, (level, pos) -> {
		BlockState below = level.getBlockState(pos.below());
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(below.getBlock());
		return id != null && CFB_NAMESPACE.equals(id.getNamespace()) && !CFB_COOKING_TABLE.equals(id);
	});

	// ME station model offsets on select blocks
	private static final Rule STURDY_UP = new Rule(1.0f / 16.0f, (level, pos) -> {
		BlockPos below = pos.below();
		BlockState belowState = level.getBlockState(below);
		if (belowState.isFaceSturdy(level, below, Direction.UP)) {
			return true;
		}
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(belowState.getBlock());
		return id != null && CFB_NAMESPACE.equals(id.getNamespace()) && "cooking_table".equals(id.getPath());
	});

	private static final Map<ResourceLocation, Rule> EXPLICIT_RULES = Map.of(
			ResourceLocation.fromNamespaceAndPath(CulinaryCompat.MODID, "bakeware"), CFB_BELOW_DOWN,
			ResourceLocation.fromNamespaceAndPath(CulinaryCompat.MODID, "ae2_kitchen_station"), STURDY_UP);

	private static Rule resolveRule(ResourceLocation blockId) {
		Rule explicit = EXPLICIT_RULES.get(blockId);
		if (explicit != null) {
			return explicit;
		}
		Block block = BuiltInRegistries.BLOCK.get(blockId);
		if (block instanceof CookingPotBlock || block instanceof SkilletBlock || block instanceof CuttingBoardBlock) {
			return CFB_BELOW_DOWN;
		}
		return null;
	}

	private ModelOffsets() {
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		Map<ModelResourceLocation, BakedModel> models = event.getModels();
		for (Map.Entry<ModelResourceLocation, BakedModel> e : models.entrySet()) {
			ModelResourceLocation key = e.getKey();
			if ("inventory".equals(key.getVariant())) {
				continue;
			}
			Rule rule = resolveRule(key.id());
			if (rule == null || e.getValue() instanceof OffsetBakedModel) {
				continue;
			}
			e.setValue(new OffsetBakedModel(e.getValue(), rule));
		}
	}

	private static BakedQuad translateY(BakedQuad q, float dy) {
		int[] v = q.getVertices().clone();
		for (int i = 0; i < 4; i++) {
			int yIdx = i * VERTEX_STRIDE_INTS + 1;
			v[yIdx] = Float.floatToRawIntBits(Float.intBitsToFloat(v[yIdx]) + dy);
		}
		return new BakedQuad(v, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade());
	}

	private record Rule(float offsetY, BiPredicate<BlockAndTintGetter, BlockPos> condition) {
	}

	static final class OffsetBakedModel implements BakedModel {
		private final BakedModel wrapped;
		private final Rule rule;
		private final ConcurrentMap<QuadKey, List<BakedQuad>> cache = new ConcurrentHashMap<>();

		OffsetBakedModel(BakedModel wrapped, Rule rule) {
			this.wrapped = wrapped;
			this.rule = rule;
		}

		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
			ModelData base = wrapped.getModelData(level, pos, state, data);
			if (rule.condition.test(level, pos)) {
				return base.derive().with(OFFSET_APPLIES, Boolean.TRUE).build();
			}
			return base;
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
				RenderType rt) {
			List<BakedQuad> base = wrapped.getQuads(state, side, rand, data, rt);
			Boolean flag = data.get(OFFSET_APPLIES);
			if (flag == null || !flag) {
				return base;
			}
			return cache.computeIfAbsent(new QuadKey(state, side, rt), k -> {
				if (base.isEmpty()) {
					return base;
				}
				List<BakedQuad> out = new ArrayList<>(base.size());
				for (BakedQuad q : base) {
					out.add(translateY(q, rule.offsetY));
				}
				return out;
			});
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
			return wrapped.getQuads(state, side, rand);
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
			return wrapped.getRenderTypes(state, rand, data);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return wrapped.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return wrapped.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return wrapped.usesBlockLight();
		}

		@Override
		public boolean isCustomRenderer() {
			return wrapped.isCustomRenderer();
		}

		@SuppressWarnings("deprecation")
		@Override
		public TextureAtlasSprite getParticleIcon() {
			return wrapped.getParticleIcon();
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			return wrapped.getParticleIcon(data);
		}

		@Override
		public ItemTransforms getTransforms() {
			return wrapped.getTransforms();
		}

		@Override
		public ItemOverrides getOverrides() {
			return wrapped.getOverrides();
		}
	}

	private record QuadKey(BlockState state, Direction side, RenderType renderType) {
	}
}
