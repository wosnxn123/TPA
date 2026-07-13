package top.craft_hello.tpa.abstracts;


import cn.handyplus.lib.adapter.HandyRunnable;
import cn.handyplus.lib.adapter.HandySchedulerUtil;
import cn.handyplus.lib.adapter.PlayerSchedulerUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import top.craft_hello.tpa.utils.SendMessageUtil;
import top.craft_hello.tpa.enums.CommandType;
import top.craft_hello.tpa.enums.PermissionType;
import top.craft_hello.tpa.exceptions.*;
import top.craft_hello.tpa.objects.Config;
import top.craft_hello.tpa.objects.LanguageConfig;
import top.craft_hello.tpa.objects.PlayerDataConfig;
import top.craft_hello.tpa.utils.LoadingConfigUtil;


import static java.util.Objects.isNull;
import static top.craft_hello.tpa.utils.LoadingConfigUtil.getConfig;


public abstract class PlayerToLocationRequest extends Request {
    protected Player requestPlayer;
    protected String requestPlayerName;
    protected String targetName;
    protected CommandType commandType;


    public PlayerToLocationRequest(CommandSender requestObject, String[] args, CommandType commandType)  {
        this.commandType = commandType;
        checkError(requestObject, args);
        if (!getConfig().isEnableTeleportDelay(requestPlayer)) {
            teleport();
            return;
        }
        setCheckMoveTimer(requestPlayer.getLocation());
        setCountdownMessageTimer(requestPlayer, targetName);
        setTimer((delay < 0L ? 3000L : delay * 1000L));
        REQUEST_QUEUE.put(requestPlayer, this);
    }

    protected void checkError() {
        if (isNull(requestPlayer) || !requestPlayer.isOnline()) throw new ErrorTargetOfflineException(requestPlayer, "null");
    }

    protected void checkError(CommandSender requestObject, String[] args)  {
        Config config = LoadingConfigUtil.getConfig();
        String command;
        PlayerDataConfig playerDataConfig;
        switch (commandType) {
            case WARP:
                command = "warp";
                if (!(requestObject instanceof Player)) throw new ErrorConsoleRestrictedException(requestObject);
                requestPlayer = (Player) requestObject;
                requestPlayerName = requestPlayer.getName();
                this.delay = LoadingConfigUtil.getConfig().getTeleportDelay(requestPlayer);
                if (!config.isEnableCommand(commandType)) throw new ErrorCommandDisabledException(requestPlayer);
                if (!config.hasPermission(requestPlayer, PermissionType.WARP)) throw new ErrorPermissionDeniedException(requestPlayer);
                if (COMMAND_DELAY_QUEUE.containsKey(requestPlayer)) throw new ErrorCommandCooldownException(requestPlayer, COMMAND_DELAY_QUEUE.get(requestPlayer));
                if (REQUEST_QUEUE.containsKey(requestPlayer)) throw new ErrorRequestPendingException(requestPlayer);
                if (args.length > 1) throw new ErrorSyntaxWarpException(requestPlayer, command);
                targetName = args[args.length - 1];
                if (!LoadingConfigUtil.getWarpConfig().containsWarpLocation(targetName)) throw new ErrorWarpNotFoundException(requestPlayer, targetName);
                location = LoadingConfigUtil.getWarpConfig().getWarpLocation(requestPlayer, targetName);
                break;
            case HOME:
                command = "home";
                if (!(requestObject instanceof Player)) throw new ErrorConsoleRestrictedException(requestObject);
                requestPlayer = (Player) requestObject;
                requestPlayerName = requestPlayer.getName();
                this.delay = LoadingConfigUtil.getConfig().getTeleportDelay(requestPlayer);
                if (!config.isEnableCommand(commandType)) throw new ErrorCommandDisabledException(requestPlayer);
                if (!config.hasPermission(requestPlayer, PermissionType.HOME)) throw new ErrorPermissionDeniedException(requestPlayer);
                if (COMMAND_DELAY_QUEUE.containsKey(requestPlayer)) throw new ErrorCommandCooldownException(requestPlayer, COMMAND_DELAY_QUEUE.get(requestPlayer));
                if (REQUEST_QUEUE.containsKey(requestPlayer)) throw new ErrorRequestPendingException(requestPlayer);
                if (args.length > 1) throw new ErrorSyntaxHomeException(requestPlayer, command);
                playerDataConfig = PlayerDataConfig.getPlayerData(requestPlayer);
                if (args.length == 0){
                    location = playerDataConfig.getHomeLocation();
                    targetName = playerDataConfig.getDefaultHomeName();
                    break;
                }
                targetName = args[args.length - 1];
                location = PlayerDataConfig.getPlayerData(requestPlayer).getHomeLocation(targetName);
                break;
            case SPAWN:
                if (!(requestObject instanceof Player)) throw new ErrorConsoleRestrictedException(requestObject);
                requestPlayer = (Player) requestObject;
                requestPlayerName = requestPlayer.getName();
                this.delay = LoadingConfigUtil.getConfig().getTeleportDelay(requestPlayer);
                if (!config.isEnableCommand(commandType)) throw new ErrorCommandDisabledException(requestPlayer);
                if (!config.hasPermission(requestPlayer, PermissionType.SPAWN)) throw new ErrorPermissionDeniedException(requestPlayer);
                if (COMMAND_DELAY_QUEUE.containsKey(requestPlayer)) throw new ErrorCommandCooldownException(requestPlayer, COMMAND_DELAY_QUEUE.get(requestPlayer));
                if (REQUEST_QUEUE.containsKey(requestPlayer)) throw new ErrorRequestPendingException(requestPlayer);
                if (!LoadingConfigUtil.getSpawnConfig().containsSpawnLocation()) throw new ErrorSpawnNotSetException(requestPlayer);
                location = LoadingConfigUtil.getSpawnConfig().getSpawnLocation(requestPlayer);
                targetName = "spawn_name";
                break;
            case BACK:
                if (!(requestObject instanceof Player)) throw new ErrorConsoleRestrictedException(requestObject);
                requestPlayer = (Player) requestObject;
                requestPlayerName = requestPlayer.getName();
                this.delay = LoadingConfigUtil.getConfig().getTeleportDelay(requestPlayer);
                if (!config.isEnableCommand(commandType)) throw new ErrorCommandDisabledException(requestPlayer);
                if (!config.hasPermission(requestPlayer, PermissionType.BACK)) throw new ErrorPermissionDeniedException(requestPlayer);
                if (COMMAND_DELAY_QUEUE.containsKey(requestPlayer)) throw new ErrorCommandCooldownException(requestPlayer, COMMAND_DELAY_QUEUE.get(requestPlayer));
                if (REQUEST_QUEUE.containsKey(requestPlayer)) throw new ErrorRequestPendingException(requestPlayer);
                location = PlayerDataConfig.getPlayerData(requestPlayer).getLastLocation();
                targetName = "last_location";
                break;
            case RTP:
                random.setSeed(System.currentTimeMillis());
                if (!(requestObject instanceof Player)) throw new ErrorConsoleRestrictedException(requestObject);
                requestPlayer = ((Player) requestObject);
                requestPlayerName = requestPlayer.getName();
                this.delay = LoadingConfigUtil.getConfig().getTeleportDelay(requestPlayer);
                if (!config.isEnableCommand(commandType)) throw new ErrorCommandDisabledException(requestPlayer);
                if (!config.hasPermission(requestPlayer, PermissionType.RTP)) throw new ErrorPermissionDeniedException(requestPlayer);
                if (COMMAND_DELAY_QUEUE.containsKey(requestPlayer)) throw new ErrorCommandCooldownException(requestPlayer, COMMAND_DELAY_QUEUE.get(requestPlayer));
                if (REQUEST_QUEUE.containsKey(requestPlayer)) throw new ErrorRequestPendingException(requestPlayer);
                targetName = "rtp_name";
                World world = requestPlayer.getWorld();
                if (config.isRtpDisableWorld(world)) throw new ErrorWorldDisabledException(requestPlayer);
                SendMessageUtil.generateRandomLocationMessage(requestPlayer);
                if (config.isEnableTitleMessage()) {
                    SendMessageUtil.titleGenerateRandomLocationMessage(requestPlayer);
                    if (config.isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }
                Location playerLocation = requestPlayer.getLocation();
                if (HandySchedulerUtil.isFolia()) {
                    // Folia/Canvas: getHighestBlockYAt/getBlockAt read the chunk synchronously
                    // and must execute on the region thread owning the target chunk. Load the
                    // chunk asynchronously and read the height inside the (region-thread)
                    // callback. The RTP rtpTimer in teleport() polls `location` until it is
                    // non-null, so resolving it asynchronously is safe.
                    generateRtpLocationAsync(world, playerLocation, config);
                } else {
                    // Paper/Bukkit: original synchronous behaviour, no overhead.
                    location = playerLocation;
                    while (true) {
                        int limitX = config.getRtpLimitX();
                        int limitZ = config.getRtpLimitZ();
                        double x;
                        double z;
                        int y;
                        switch (world.getEnvironment()){
                            case NETHER:
                                x = random.nextDouble(location.getX() - limitX,location.getX() + limitX);
                                z = random.nextDouble(location.getZ() - limitZ, location.getZ() + limitZ);
                                location.setX(x);
                                location.setZ(z);
                                y = world.getHighestBlockYAt((int) location.getX(), (int) location.getZ(), HeightMap.WORLD_SURFACE);
                                location.setY(y);
                                break;
                            case THE_END:
                                x = random.nextDouble(-100,100);
                                z = random.nextDouble(-100,100);
                                location.setX(x);
                                location.setZ(z);
                                y = world.getHighestBlockYAt((int) location.getX(), (int) location.getZ(), HeightMap.WORLD_SURFACE);
                                location.setY(y);
                                break;
                            default:
                                x = random.nextDouble(location.getX() - limitX,location.getX() + limitX);
                                z = random.nextDouble(location.getZ() - limitZ, location.getZ() + limitZ);
                                location.setX(x);
                                location.setZ(z);
                                y = world.getHighestBlockYAt((int) location.getX(), (int) location.getZ(), HeightMap.WORLD_SURFACE);
                                location.setY(y);
                        }
                        Block feetBlock = world.getBlockAt(location);
                        if (feetBlock.getType().isSolid()) break;
                    }
                }
                break;
            default:
                throw new ErrorRuntimeException(requestObject, "在 objects.PlayerToLocationRequest : 35行，请联系开发者（https://github.com/WarSkyGod/TPA/issues）");
        }
    }

    /**
     * Folia/Canvas 兼容：异步加载目标区块并在区块所在 region 线程读取高度。
     * 不能同步调用 getHighestBlockYAt/getBlockAt（会抛 "Cannot retrieve chunk asynchronously"）。
     * 通过 getChunkAtAsync 异步获取区块，在回调（区块所在 region 线程）里读取最高方块
     * 高度并判断是否为实心方块。若不是则重新随机坐标并重试，最多尝试 {@value #RTP_MAX_ATTEMPTS} 次。
     * 成功后写入 {@link #location}，由 RTP 的 rtpTimer 轮询触发传送（见 teleport()）。
     */
    private static final int RTP_MAX_ATTEMPTS = 64;
    private volatile boolean rtpGenerationActive;

    private void generateRtpLocationAsync(World world, Location origin, Config config) {
        rtpGenerationActive = true;
        generateRtpLocationAsync(world, origin, config, 0);
    }

    private void generateRtpLocationAsync(final World world, final Location origin, final Config config, final int attempt) {
        if (!rtpGenerationActive || !requestPlayer.isOnline()) {
            rtpGenerationActive = false;
            return;
        }
        if (attempt >= RTP_MAX_ATTEMPTS) {
            // 超过最大尝试次数仍无合适位置：保持 location 为 null，
            // rtpTimer 会在超时后抛出 RtpFailedException 通知玩家。
            rtpGenerationActive = false;
            return;
        }
        int limitX = config.getRtpLimitX();
        int limitZ = config.getRtpLimitZ();
        final double x;
        final double z;
        switch (world.getEnvironment()) {
            case NETHER:
                x = random.nextDouble(origin.getX() - limitX, origin.getX() + limitX);
                z = random.nextDouble(origin.getZ() - limitZ, origin.getZ() + limitZ);
                break;
            case THE_END:
                x = random.nextDouble(-100, 100);
                z = random.nextDouble(-100, 100);
                break;
            default:
                x = random.nextDouble(origin.getX() - limitX, origin.getX() + limitX);
                z = random.nextDouble(origin.getZ() - limitZ, origin.getZ() + limitZ);
                break;
        }
        final int blockX = (int) x;
        final int blockZ = (int) z;
        // getChunkAtAsync 接收区块坐标（block >> 4），回调在拥有该区块的 region 线程执行
        Consumer<Chunk> onChunkLoaded = new Consumer<Chunk>() {
            @Override
            public void accept(Chunk chunk) {
                if (!rtpGenerationActive || !requestPlayer.isOnline()) {
                    rtpGenerationActive = false;
                    return;
                }
                try {
                    int y = world.getHighestBlockYAt(blockX, blockZ, HeightMap.WORLD_SURFACE);
                    Block feetBlock = world.getBlockAt(blockX, y, blockZ);
                    if (feetBlock.getType().isSolid()) {
                        location = new Location(world, x, y, z);
                        rtpGenerationActive = false;
                    } else {
                        // 重新随机一个坐标并异步重试
                        generateRtpLocationAsync(world, origin, config, attempt + 1);
                    }
                } catch (Throwable ignored) {
                    generateRtpLocationAsync(world, origin, config, attempt + 1);
                }
            }
        };
        world.getChunkAtAsync(blockX >> 4, blockZ >> 4, onChunkLoaded);
    }

    protected void setTimer(long delay){
        HandyRunnable timer = new HandyRunnable() {
            @Override
            public void run() {
                try {
                    // 执行逻辑
                    teleport();
                } catch (Exception ignored){
                    REQUEST_QUEUE.remove(requestPlayer);
                    rtpGenerationActive = false;
                    this.cancel();
                }
            }
        };
        this.timer = timer;
        HandySchedulerUtil.runTaskLaterAsynchronously(timer, delay / 50L);
    }

    protected void isMove(@NotNull Location lastLocation){
        if (requestPlayer.getLocation().getX() != lastLocation.getX() || requestPlayer.getLocation().getY() != lastLocation.getY() || requestPlayer.getLocation().getZ() != lastLocation.getZ()){
            REQUEST_QUEUE.remove(requestPlayer);
            rtpGenerationActive = false;
            timer.cancel();
            checkMoveTimer.cancel();
            countdownMessageTimer.cancel();
            if (LoadingConfigUtil.getConfig().isEnableTitleMessage()){
                LanguageConfig language = LanguageConfig.getLanguage(requestPlayer);
                String title = language.getFormatMessage("teleport.canceled.self");
                if (LoadingConfigUtil.getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                requestPlayer.sendTitle(title, "");
            }
            SendMessageUtil.move(requestPlayer, requestPlayer);
        }
    }

    protected void setCheckMoveTimer(@NotNull Location lastLocation){
        HandyRunnable checkMoveTimer = new HandyRunnable() {
            long sec = delay * 20;
            @Override
            public void run() {
                try {
                    // 执行逻辑
                    isMove(lastLocation);
                    if (--sec < 0){
                        this.cancel();
                    }
                } catch (Exception ignored) {
                    this.cancel();
                }
            }
        };
        this.checkMoveTimer = checkMoveTimer;
        HandySchedulerUtil.runTaskTimerAsynchronously(checkMoveTimer, 0, 1);
    }

    protected void teleport()  {
        if (getConfig().isEnableTeleportDelay(requestPlayer)) {
            REQUEST_QUEUE.remove(requestPlayer);
            checkMoveTimer.cancel();
        }
        checkError();
        switch (commandType){
            case WARP:
                if (getConfig().isEnableTitleMessage()) {
                    SendMessageUtil.titleCountdownOverMessage(requestPlayer, targetName);
                    if (getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }
                SendMessageUtil.tpToWarpMessage(requestPlayer, targetName);
                if (!getConfig().isNonTpaOrTphereDisableCheck() && getConfig().isEnableCommandDelay(requestPlayer))
                    setCommandTimer(requestPlayer, getConfig().getCommandDelay(requestPlayer));
                break;
            case HOME:
                if (getConfig().isEnableTitleMessage()) {
                    SendMessageUtil.titleCountdownOverMessage(requestPlayer, targetName);
                    if (getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }
                SendMessageUtil.tpToHomeMessage(requestPlayer, targetName);
                if (!getConfig().isNonTpaOrTphereDisableCheck() && getConfig().isEnableCommandDelay(requestPlayer))
                    setCommandTimer(requestPlayer, getConfig().getCommandDelay(requestPlayer));
                break;
            case SPAWN:
                if (getConfig().isEnableTitleMessage()) {
                    Bukkit.getConsoleSender().sendMessage(targetName);
                    SendMessageUtil.titleCountdownOverMessage(requestPlayer, targetName);
                    if (getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }
                SendMessageUtil.backSpawnSuccessMessage(requestPlayer);
                if (!getConfig().isNonTpaOrTphereDisableCheck() && getConfig().isEnableCommandDelay(requestPlayer))
                    setCommandTimer(requestPlayer, getConfig().getCommandDelay(requestPlayer));
                break;
            case BACK:
                if (getConfig().isEnableTitleMessage()) {
                    SendMessageUtil.titleCountdownOverMessage(requestPlayer, targetName);
                    if (getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                }
                SendMessageUtil.backLastLocationSuccessMessage(requestPlayer);
                if (!getConfig().isNonTpaOrTphereDisableCheck() && getConfig().isEnableCommandDelay(requestPlayer))
                    setCommandTimer(requestPlayer, getConfig().getCommandDelay(requestPlayer));
                break;
            case RTP:
                HandyRunnable rtpTimer = new HandyRunnable() {
                    long sec = 200;
                    @Override
                    public void run() {
                        try {
                            if (!isNull(location)){
                                rtpGenerationActive = false;
                                teleport(requestPlayer, location);
                                if (getConfig().isEnableTitleMessage()) {
                                    SendMessageUtil.titleCountdownOverMessage(requestPlayer, targetName);
                                    if (getConfig().isEnableSound()) PlayerSchedulerUtil.playSound(requestPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                }
                                SendMessageUtil.rtpSuccessMessage(requestPlayer);
                                this.cancel();
                            }
                            if (--sec < 0) {
                                rtpGenerationActive = false;
                                throw new RtpFailedException(requestPlayer);
                            }
                        } catch (Exception ignored) {
                            rtpGenerationActive = false;
                            this.cancel();
                        }
                    }
                };
                HandySchedulerUtil.runTaskTimerAsynchronously(rtpTimer, 0, 1);
                if (!getConfig().isNonTpaOrTphereDisableCheck() && getConfig().isEnableCommandDelay(requestPlayer))
                    setCommandTimer(requestPlayer, getConfig().getCommandDelay(requestPlayer));
                return;
        }
        teleport(requestPlayer, location);
    }

    @Override
    public void tpaccept()  {
        throw new ErrorNoPendingRequestException(requestPlayer);
    }

    @Override
    public void tpdeny()  {
        throw new ErrorNoPendingRequestException(requestPlayer);
    }

}
