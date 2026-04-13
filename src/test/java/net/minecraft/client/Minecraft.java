package net.minecraft.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;

public class Minecraft {
    public EntityPlayerSP player;
    public WorldClient world;
    public PlayerControllerMP playerController;

    private static final Minecraft instance = new Minecraft();

    public static Minecraft getMinecraft() {
        return instance;
    }
}
