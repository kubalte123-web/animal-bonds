package pl.kubalte.gospodarstwo.client.goblin;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class GoblinRenderState extends HumanoidRenderState {
    public boolean pack, tame, considering, resting;
    public int race, action;
    public float actionAge;
}
