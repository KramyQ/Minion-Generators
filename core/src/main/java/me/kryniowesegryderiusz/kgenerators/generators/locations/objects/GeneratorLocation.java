package me.kryniowesegryderiusz.kgenerators.generators.locations.objects;

import java.util.UUID;

import javax.annotation.Nullable;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import me.kryniowesegryderiusz.kgenerators.dependencies.objects.GeneratedOraxenBlock;
import me.kryniowesegryderiusz.kgenerators.generators.generator.objects.GeneratedBlock;
import me.kryniowesegryderiusz.kgenerators.utils.objects.CustomBlockData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import lombok.Setter;
import me.kryniowesegryderiusz.kgenerators.Main;
import me.kryniowesegryderiusz.kgenerators.api.interfaces.IGeneratorLocation;
import me.kryniowesegryderiusz.kgenerators.api.objects.AbstractGeneratedObject;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.BentoBoxHook;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.FactionsUUIDHook;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.LandsHook;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.PlotSquaredHook;
import me.kryniowesegryderiusz.kgenerators.dependencies.hooks.SuperiorSkyblock2Hook;
import me.kryniowesegryderiusz.kgenerators.generators.generator.enums.GeneratorType;
import me.kryniowesegryderiusz.kgenerators.generators.generator.objects.Generator;
import me.kryniowesegryderiusz.kgenerators.generators.locations.PlacedGeneratorsManager.ChunkInfo;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.GeneratorLocationActionHandler;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.GeneratorLocationPickUpHandler;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.GeneratorLocationPlaceHandler;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.GeneratorLocationRegenerateHandler;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.GeneratorLocationRemoveHandler;
import me.kryniowesegryderiusz.kgenerators.generators.locations.handlers.enums.InteractionType;
import me.kryniowesegryderiusz.kgenerators.generators.players.objects.GeneratorPlayer;
import me.kryniowesegryderiusz.kgenerators.lang.Lang;
import me.kryniowesegryderiusz.kgenerators.lang.enums.Message;
import me.kryniowesegryderiusz.kgenerators.logger.Logger;
import me.kryniowesegryderiusz.kgenerators.xseries.XMaterial;

public class GeneratorLocation implements IGeneratorLocation {

    private static GeneratorLocationPickUpHandler pickUpHandler = new GeneratorLocationPickUpHandler();
    private static GeneratorLocationPlaceHandler placeHandler = new GeneratorLocationPlaceHandler();
    private static GeneratorLocationRegenerateHandler regenerateHandler = new GeneratorLocationRegenerateHandler();
    private static GeneratorLocationRemoveHandler removeHandler = new GeneratorLocationRemoveHandler();
    private static GeneratorLocationActionHandler actionHandler = new GeneratorLocationActionHandler();

    @Setter
    @Getter
    private int id;
    @Getter
    private Generator generator;
    @Getter
    private GeneratorPlayer owner;
    @Getter
    private Location location;


    private Location hologramLocation;
    private Location originBlockLocation;
    private UUID hologramUUID = UUID.randomUUID();

    @Getter
    private ChunkInfo chunkInfo;

    @Setter
    @Getter
    AbstractGeneratedObject lastGeneratedObject;

    @Setter
    @Getter
    AbstractGeneratedObject blockToGenerate;

    @Setter
    @Getter
    String blockToGenerateName;

    @Setter
    @Getter
    Integer generationDelay = 20;

    /**
     * Creates GeneratorLocation. **Note that you probably should use
     * GeneratorLocation#save() method**
     *
     * @param id              - generatorLocation database id (-1 if not yet in database)
     * @param Generator       generator
     * @param Location        location
     * @param Chunk           - chunk, where location
     * @param GeneratorPlayer owner - nullable
     * @param ago             AbstractGeneratedObject - nullable
     */
    public GeneratorLocation(int id, Generator generator, Location location, ChunkInfo chunkInfo, @Nullable GeneratorPlayer owner, @Nullable AbstractGeneratedObject ago) {
        this.id = id;
        this.generator = generator;
        this.owner = owner;
        this.location = location;

        this.hologramLocation = location.clone().add(0.5, 0, 0.5);

        if (generator.getType() == GeneratorType.DOUBLE)
            this.hologramLocation.add(0, 1, 0);

        if (getGenerator().getPlaceholder() != null)
            this.hologramLocation.add(0, 1, 0);

        this.chunkInfo = chunkInfo;

        this.setLastGeneratedObject(ago);

        Location originBlockLocation = this.getOriginBlockLocation();
        Block originBlock = originBlockLocation.getBlock();
        if (OraxenBlocks.isOraxenNoteBlock(originBlock)) {
            Mechanic oraxenBlock = OraxenBlocks.getOraxenBlock(originBlockLocation);
            GeneratedOraxenBlock blockBelow = new GeneratedOraxenBlock();
            blockToGenerateName = oraxenBlock.getItemID();
            blockBelow.setMaterial("oraxen:" + oraxenBlock.getItemID());
            blockToGenerate = blockBelow;
            Integer expectedDelay = Main.getSettings().getMaterialsDelays().get("oraxen:" + oraxenBlock.getItemID().toLowerCase());
            if (expectedDelay == null) {
                blockToGenerate = null;
            } else {
                generationDelay = expectedDelay;
            }
        } else {
            GeneratedBlock blockBelow = new GeneratedBlock();
            blockToGenerateName = originBlock.getBlockData().getMaterial().toString();
            blockBelow.setCustomBlockData(CustomBlockData.load(originBlock.getBlockData().getMaterial().toString(), "Generators file: GeneratedBlock:"));
            blockToGenerate = blockBelow;
            Integer expectedDelay = Main.getSettings().getMaterialsDelays().get((String) originBlock.getBlockData().getMaterial().toString().toLowerCase());
            if (expectedDelay == null) {
                blockToGenerate = null;
            } else {
                generationDelay = expectedDelay;
            }
        }

//		if(generator.getType() == GeneratorType.DOUBLE){
//				this.setGeneratingBlock();
//		}
    }

    /*
     * Getting GeneratorLocation info
     */

    // api
    public Location getGeneratedBlockLocation() {
        if (this.getGenerator().getType() == GeneratorType.SINGLE)
            return location;
        else
            return location.clone().add(0, 1, 0);
    }

    // api
    public boolean isBlockPossibleToMine(Location location) {
        return this.getGeneratedBlockLocation().equals(location) &&
                (this.getGenerator().getPlaceholder() == null || !this.getGenerator().getPlaceholder().getItem().equals(Main.getMultiVersion().getBlocksUtils().getItemStackByBlock(location.getBlock())));
    }

    // api

    /**
     * Checks if player is permitted to mine generated block. You probably wanna use
     * {@link #isBlockPossibleToMine(Location)} at first
     *
     * @return wheather its possible to mine
     */
    public boolean isPermittedToMine(Player player) {
        String permission = "kgenerators.mine." + this.getGenerator().getId();
        if (!player.hasPermission(permission)) {
            Lang.getMessageStorage().send(player, Message.GENERATORS_DIGGING_NO_PERMISSION, "<permission>", permission,
                    "<generator>", this.getGenerator().getGeneratorItem().getItemMeta().getDisplayName());
            return false;
        }

        if (!BentoBoxHook.isAllowed(player, BentoBoxHook.Type.USE_FLAG, this.getGeneratedBlockLocation())
                || !SuperiorSkyblock2Hook.isAllowed(player, SuperiorSkyblock2Hook.Type.USE_FLAG,
                this.getGeneratedBlockLocation())
                || !PlotSquaredHook.isPlayerAllowedToMine(player, this.getGeneratedBlockLocation())
                || !LandsHook.isPlayerAllowedToMine(player, this.getGeneratedBlockLocation())
                || !FactionsUUIDHook.isPlayerAllowedToMine(player, this.getGeneratedBlockLocation())) {
            Lang.getMessageStorage().send(player, Message.GENERATORS_DIGGING_CANT_HERE);
            return false;
        }
        return true;
    }

    /**
     * Returns whether this generatorLocation is corrupted. More precisely if there
     * isnt any scheduled regeneration referred to it and there is nothing in place
     * of that generator.
     *
     * @return
     */
    // api
    public boolean isBroken() {
        if (!Main.getPlacedGenerators().isLoaded(this))
            return false;

        if (!Main.getSchedules().isScheduled(this)) {
            if (Main.getMultiVersion().getBlocksUtils().isAir(this.getGeneratedBlockLocation().getBlock())) {
                return true;
            } else {
                if (this.getGenerator().getPlaceholder() != null) {
                    ItemStack itemBlock = Main.getMultiVersion().getBlocksUtils().getItemStackByBlock(this.getGeneratedBlockLocation().getBlock());
                    if (itemBlock.isSimilar(this.getGenerator().getPlaceholder().getItem())) {
                        for (AbstractGeneratedObject ago : this.getGenerator().getGeneratedObjects()) {
                            if (ago.isBlockSimilar(itemBlock))
                                return false;
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isLoaded() {
        return Main.getPlacedGenerators().isLoaded(this);
    }

    /*
     * GeneratorLocation actions
     */

    // api
    public void regenerateGenerator() {
        regenerateHandler.handle(this);
    }

    // api
    public void scheduleGeneratorRegeneration() {
        Main.getSchedules().schedule(this, false);
    }

    // api
    public void removeGenerator(boolean drop, @Nullable Player player) {
        removeHandler.handle(this, drop, player);
    }

    /**
     * Places generator and saves it
     *
     * @return whether placing was successful
     */
    public boolean placeGenerator(Player player) {
        return placeHandler.handle(this, player, false);
    }

    /**
     * Places generator and saves it For commands/api usage
     *
     * @return whether placing was successful
     */
    public boolean placeGenerator(CommandSender sender, boolean generateGeneratorBlock) {
        return placeHandler.handle(this, sender, generateGeneratorBlock);
    }

    public void pickUpGenerator(Player player) {
        pickUpHandler.handle(this, player);
    }

    /**
     * @return true if event should be cancelled
     */
    public boolean handleAction(InteractionType usedActionType, Player player) {
        return actionHandler.handle(this, usedActionType, player);
    }

    /**
     * Changes this generatorLocation to another generator
     *
     * @param Generator generator
     */
    // api
    public void changeTo(Generator generator) {
        Logger.info("Generator " + this.generator.getId() + " placed in " + this.toStringLocation()
                + " was transformed to " + generator.getId());
        Main.getSchedules().remove(this);

        if (this.owner != null) {
            this.getOwner().removeGeneratorFromPlayer(this.getGenerator());
            this.getOwner().addGeneratorToPlayer(generator);
        }

        this.generator = generator;
        if (this.getGenerator().getType() == GeneratorType.SINGLE) {
            Main.getMultiVersion().getBlocksUtils().setBlock(this.location, XMaterial.AIR);
        } else {
            Main.getMultiVersion().getBlocksUtils().setBlock(this.location, this.getGenerator().getGeneratorItem());
            Main.getMultiVersion().getBlocksUtils().setBlock(this.location.clone().add(0, 1, 0), XMaterial.AIR);
        }

        Main.getDatabases().getDb().saveGenerator(this);

        this.regenerateGenerator();

    }

    public int getLastGeneratedObjectId() {
        return this.getGenerator().getGeneratedObjectId(this.lastGeneratedObject);
    }

    public boolean isReadyForRegeneration() {
        return this.lastGeneratedObject == null ? true : this.lastGeneratedObject.isReady();
    }

    /*
     * Hologram
     */

    public Location getHologramLocation(int lines) {
        return this.hologramLocation.clone().add(0, 0.2 * (lines + 1), 0);
    }

    public Location getOriginBlockLocation() {
        return this.location.clone().add(0, -1, 0);
    }

    public String getHologramUUID() {
        return "kgenerators_" + this.hologramUUID.toString();
    }

    /*
     * Other
     */

    @Override
    public String toString() {
        if (this.owner != null)
            return "(" + this.id + ") " + this.generator.getId() + " owned by " + this.owner.getName() + " placed in " + toStringLocation()
                    + " (" + this.getChunkInfo().toString() + ")";
        else
            return "(" + this.id + ") " + this.generator.getId() + " owned by no one" + " placed in " + toStringLocation() + " ("
                    + this.getChunkInfo().toString() + ")";
    }

    public String toStringLocation() {
        return "world " + this.location.getWorld().getName() + " at " + String.valueOf(this.location.getX()) + ", "
                + String.valueOf(this.location.getY()) + ", " + String.valueOf(this.location.getZ());
    }
}
