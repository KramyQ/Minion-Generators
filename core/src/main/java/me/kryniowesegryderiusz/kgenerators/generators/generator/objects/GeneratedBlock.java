package me.kryniowesegryderiusz.kgenerators.generators.generator.objects;

import java.util.Map;

import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import me.kryniowesegryderiusz.kgenerators.Main;
import me.kryniowesegryderiusz.kgenerators.api.interfaces.IGeneratorLocation;
import me.kryniowesegryderiusz.kgenerators.api.objects.AbstractGeneratedObject;
import me.kryniowesegryderiusz.kgenerators.dependencies.enums.Dependency;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.SuperiorSkyblock2Hook;
import me.kryniowesegryderiusz.kgenerators.utils.objects.CustomBlockData;

public class GeneratedBlock extends AbstractGeneratedObject {

	public GeneratedBlock() {
		super("block");
	}

	@Getter @Setter
	private CustomBlockData customBlockData;
	
	@Getter private boolean physics = true;
	
	@Override
	protected boolean compareSameType(AbstractGeneratedObject generatedObject) {
		GeneratedBlock gb = (GeneratedBlock) generatedObject;
		return customBlockData.getXMaterial() == gb.getCustomBlockData().getXMaterial();
	}

	@Override
	protected boolean loadTypeSpecific(Map<?, ?> generatedObjectConfig) {
		if (generatedObjectConfig.containsKey("physics"))
			this.physics = (boolean) generatedObjectConfig.get("physics");
		if (generatedObjectConfig.containsKey("material")) {
			this.customBlockData = CustomBlockData.load((String) generatedObjectConfig.get("material"), "Generators file: GeneratedBlock:");
			return true;
		}
		return false;
	}

	@Override
	public void regenerate(IGeneratorLocation generatorLocation) {
		
		this.customBlockData.setBlock(generatorLocation.getGeneratedBlockLocation(), physics);

		if (Main.getDependencies().isEnabled(Dependency.SUPERIOR_SKYBLOCK_2))
			  SuperiorSkyblock2Hook.handleBlockPlace(generatorLocation.getGeneratedBlockLocation().getBlock());
	}

	@Override
	public ItemStack getGuiItem() {
		return this.getCustomBlockData().getItem();
	}

	@Override
	protected String toStringSpecific() {
		return this.getCustomBlockData().toString();
	}
	
	@Override
	public boolean isBlockSimilar(ItemStack itemStackBlock) {
		return this.customBlockData.isSimilar(itemStackBlock);
	}
}
