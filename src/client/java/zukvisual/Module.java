package zukvisual;

public class Module {
    public final String name;
    public final String desc;
    public boolean enabled;
    public float anim;   // анимация в списке на HUD
    public float toggle; // анимация переключателя в меню
    private int w = -1;

    /** Ширина названия, считается один раз. */
    public int width(net.minecraft.client.font.TextRenderer tr) {
        if (w < 0) w = tr.getWidth(name);
        return w;
    }

    public Module(String name, String desc, boolean enabled) {
        this.name = name;
        this.desc = desc;
        this.enabled = enabled;
    }
}
