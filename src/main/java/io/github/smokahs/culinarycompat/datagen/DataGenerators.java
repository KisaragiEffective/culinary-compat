package io.github.smokahs.culinarycompat.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import org.jetbrains.annotations.Nullable;

import io.github.smokahs.culinarycompat.CulinaryCompat;

@net.neoforged.fml.common.EventBusSubscriber(modid = CulinaryCompat.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
	private DataGenerators() {
	}

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		BlockTags blockTags = new BlockTags(packOutput, lookupProvider, existingFileHelper);
		generator.addProvider(event.includeServer(), blockTags);
		generator.addProvider(event.includeServer(),
				new ItemTags(packOutput, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
		generator.addProvider(event.includeServer(), new Recipes(packOutput, lookupProvider));

		generator.addProvider(event.includeClient(), new Lang(packOutput));
	}

	static final class BlockTags extends BlockTagsProvider {
		BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
				@Nullable ExistingFileHelper existingFileHelper) {
			super(output, lookupProvider, CulinaryCompat.MODID, existingFileHelper);
		}

		@Override
		protected void addTags(HolderLookup.Provider provider) {
		}
	}

	static final class ItemTags extends ItemTagsProvider {
		ItemTags(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider,
				CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
				@Nullable ExistingFileHelper existingFileHelper) {
			super(packOutput, lookupProvider, blockTags, CulinaryCompat.MODID, existingFileHelper);
		}

		@Override
		protected void addTags(HolderLookup.Provider provider) {
		}
	}

	static final class Lang extends LanguageProvider {
		Lang(PackOutput output) {
			super(output, CulinaryCompat.MODID, "en_us");
		}

		@Override
		protected void addTranslations() {
		}
	}

	static final class Recipes extends RecipeProvider {
		Recipes(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
			super(packOutput, provider);
		}

		@Override
		protected void buildRecipes(RecipeOutput consumer) {
		}
	}
}
