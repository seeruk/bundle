package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.names.PlayerNames;
import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import dev.seeruk.bundle.leaders.LeadersConfig.Category;
import dev.seeruk.bundle.leaders.mixin.MannequinInvoker;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.leaders.SeersLeaders.LOGGER;

/**
 * Mannequins that show who's sitting in a {@link Seat}, with their skin, name, and total. Each one
 * only remembers its seat.
 */
public final class LeaderMannequins {
    private static final AttachmentType<Seat> SEAT = AttachmentRegistry.createPersistent(
        SeersLeaders.id("leader_mannequin"),
        Seat.CODEC
    );

    // Leader mannequins that are loaded, by UUID. Concurrent, as it's shared across levels.
    private static final Map<UUID, Mannequin> LOADED = new ConcurrentHashMap<>();

    // Categories a mannequin's seat names that aren't in the config, so each is only warned about once
    private static final Set<String> UNKNOWN = ConcurrentHashMap.newKeySet();

    private LeaderMannequins() {}

    // Only here to make sure the class (and its attachment) is registered on mod initialization.
    public static void init() {}

    /**
     * Places a mannequin showing the seat on top of the block the player is looking at, facing them.
     */
    public static PlaceResult place(ServerPlayer player, LeadersConfig config, Seat seat) {
        if (config.category(seat.category()).isEmpty()) {
            return new PlaceResult.UnknownCategory(seat.category());
        }

        if (!(player.pick(player.blockInteractionRange(), 1, false) instanceof BlockHitResult hit)
            || hit.getType() == HitResult.Type.MISS) {
            return new PlaceResult.NoBlockInSight();
        }

        final var level = player.level();
        final var block = hit.getBlockPos();
        final var shape = level.getBlockState(block).getCollisionShape(level, block);
        final var x = block.getX() + 0.5;
        final var y = block.getY() + (shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y));
        final var z = block.getZ() + 0.5;

        if (!level.noCollision(EntityTypes.MANNEQUIN.getSpawnAABB(x, y, z))) {
            return new PlaceResult.Obstructed();
        }

        final var mannequin = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.COMMAND);
        if (mannequin == null) {
            return new PlaceResult.Obstructed(); // Only if mannequins are disabled in the world
        }

        // Facing back towards the player, snapped to the nearest 45°
        final var yaw = Mth.wrapDegrees(Math.round((player.getYRot() + 180) / 45) * 45f);
        mannequin.snapTo(x, y, z, yaw, 0);
        mannequin.setYBodyRot(yaw);
        mannequin.setYHeadRot(yaw);
        mannequin.setPermanentlyInvulnerable(true);
        ((MannequinInvoker) mannequin).invokeSetImmovable(true);
        mannequin.setAttached(SEAT, seat);
        show(mannequin, seat, config);

        level.addFreshEntity(mannequin);
        return new PlaceResult.Placed(mannequin, seat);
    }

    /**
     * Removes the leader mannequin the player is looking at.
     */
    public static RemoveResult remove(ServerPlayer player) {
        final var hit = ProjectileUtil.getHitResultOnViewVector(
            player,
            entity -> entity.hasAttached(SEAT),
            player.entityInteractionRange()
        );
        if (!(hit instanceof EntityHitResult entityHit)) {
            return new RemoveResult.NotLookingAtLeaderMannequin();
        }

        final var mannequin = entityHit.getEntity();
        final var seat = mannequin.getAttached(SEAT);
        mannequin.discard();
        return new RemoveResult.Removed(seat);
    }

    /**
     * The seat a mannequin shows, if it's a leader mannequin.
     */
    public static Optional<Seat> seat(Entity entity) {
        return Optional.ofNullable(entity.getAttached(SEAT));
    }

    public static void loaded(Entity entity, LeadersConfig config) {
        if (entity instanceof Mannequin mannequin && mannequin.hasAttached(SEAT)) {
            LOADED.put(mannequin.getUUID(), mannequin);
            show(mannequin, mannequin.getAttached(SEAT), config);
        }
    }

    public static void unloaded(Entity entity) {
        LOADED.remove(entity.getUUID());
    }

    /**
     * Brings every loaded leader mannequin up to date with who's in its seat.
     */
    public static void showAll(LeadersConfig config) {
        LOADED.values().forEach(mannequin -> show(mannequin, mannequin.getAttached(SEAT), config));
    }

    public static void clear() {
        LOADED.clear();
        UNKNOWN.clear();
    }

    private static void show(Mannequin mannequin, Seat seat, LeadersConfig config) {
        final var category = config.category(seat.category());
        if (category.isEmpty()) {
            if (UNKNOWN.add(seat.category())) {
                LOGGER.warn("A leader mannequin shows category '{}', which isn't in the config, so it's left as it is", seat.category());
            }
            return;
        }

        final var standing = Leaders.standing(seat);
        showText(mannequin, seat, category.get(), standing);
        showLook(mannequin, category.get(), standing);
    }

    // Its name and description, for whoever's in the seat, or for an empty seat
    private static void showText(Mannequin mannequin, Seat seat, Category category, Optional<Standing> standing) {
        // Who held the seat last period, shown while nobody holds it, if the category says how
        final var previous = standing.isPresent() || category.previousDescription().isEmpty()
            ? Optional.<Standing>empty()
            : Leaders.previous(mannequin.level().getServer(), seat);

        final TextTemplate name;
        final TextTemplate description;
        if (standing.isPresent()) {
            name = category.name();
            description = category.description();
        } else {
            name = category.emptyName();
            description = previous.isPresent() ? category.previousDescription().get() : category.emptyDescription();
        }

        final var values = values(mannequin, seat, category, standing.or(() -> previous));
        mannequin.setCustomName(name.format(values));
        mannequin.setCustomNameVisible(true);
        ((MannequinInvoker) mannequin).invokeSetDescription(description.format(values));
    }

    // Its skin and held item. Each is only changed when it's different, so a skin isn't looked up
    // again on every refresh. An empty seat gets the default skin, even if last period's holder is
    // named, as nobody's in it now.
    private static void showLook(Mannequin mannequin, Category category, Optional<Standing> standing) {
        final var profile = standing
            .map(found -> ResolvableProfile.createUnresolved(found.player()))
            .orElse(Mannequin.DEFAULT_PROFILE);
        if (!profile.partialProfile().id().equals(mannequin.getProfile().partialProfile().id())) {
            mannequin.setComponent(DataComponents.PROFILE, profile);
        }

        final var item = category.item().map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (!mannequin.getMainHandItem().is(item.getItem())) {
            mannequin.setItemSlot(EquipmentSlot.MAINHAND, item);
        }
    }

    private static Map<String, Component> values(
        Mannequin mannequin,
        Seat seat,
        Category category,
        Optional<Standing> standing
    ) {
        final var values = new HashMap<String, Component>();
        values.put("rank", Component.literal(String.valueOf(seat.rank())));
        values.put("category", category.displayTitle());
        standing.ifPresent(found -> {
            values.put("player", PlayerNames.displayName(mannequin.level().getServer(), found.player()));
            values.put("value", Component.literal(category.unit().format(found.value())));
        });
        return values;
    }
}
