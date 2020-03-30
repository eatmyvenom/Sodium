package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.AttackIndicator;
import net.minecraft.client.options.ParticlesOption;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("View Distance")
                        .setTooltip("The view distance controls how far away terrain will be rendered. Lower distances mean that less terrain will be " +
                                "rendered, improving frame rates.")
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.quanity("Chunks")))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("V-Sync")
                        .setTooltip("If enabled, the game's frame rate will be synchronized to the monitor's refresh rate, making for a generally smoother experience " +
                                "at the expense of overall input latency. This setting might reduce performance if your system is too slow.")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(net.minecraft.client.options.Option.VSYNC))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("FPS Limit")
                        .setTooltip("Limits the maximum number of frames per second. In effect, this will throttle the game and can be useful when you want to conserve " +
                                "battery life or multi-task between other applications.")
                        .setControl(option -> new SliderControl(option, 5, 300, 5, ControlValueFormatter.quanity("FPS")))
                        .setBinding((opts, value) -> {
                            opts.maxFps = value;
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.maxFps)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("Fullscreen")
                        .setTooltip("If enabled, the game will display in full-screen.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen) {
                                window.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen = window.isFullscreen();
                            }
                        }, (opts) -> opts.fullscreen)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Brightness")
                        .setTooltip("Controls the brightness (gamma) of the game.")
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.gamma = value * 0.01D, (opts) -> (int) (opts.gamma / 0.01D))
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Clouds")
                        .setTooltip("Controls whether or not clouds will be visible.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableClouds = value, (opts) -> opts.quality.enableClouds)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesOption.class, vanillaOpts)
                        .setName("Particles")
                        .setTooltip("Controls the maximum number of particles which can be present on screen at any one time.")
                        .setControl(opt -> new CyclingControl<>(opt, ParticlesOption.values(), new String[] { "All", "Decreased", "Minimal" }))
                        .setBinding((opts, value) -> opts.particles = value, (opts) -> opts.particles)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("View Bobbing")
                        .setTooltip("If enabled, the player's view will sway and bob when moving around. Players who suffer from motion sickness can benefit from disabling this.")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(net.minecraft.client.options.Option.VIEW_BOBBING))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName("Attack Indicator")
                        .setTooltip("Controls where the Attack Indicator is displayed on screen.")
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.values(), new String[] { "Off", "Crosshair", "Hotbar" }))
                        .setBinding((opts, value) -> opts.attackIndicator = value, (opts) -> opts.attackIndicator)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Static Fov")
                        .setTooltip("If enabled, the field of view will not change in response to potion effects or sprinting.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.staticFov = value, (opts) -> opts.quality.staticFov)
                        .build())
                .build());

        return new OptionPage("General", ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.DefaultGraphicsQuality.class, vanillaOpts)
                        .setName("Graphics Quality")
                        .setTooltip("The default graphics quality controls some legacy options and is necessary for mod compatibility. If the options below are left to " +
                                "\"Default\", they will use this setting.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.DefaultGraphicsQuality.values()))
                        .setBinding(
                                (opts, value) -> opts.fancyGraphics = value == SodiumGameOptions.DefaultGraphicsQuality.FANCY,
                                opts -> (opts.fancyGraphics ? SodiumGameOptions.DefaultGraphicsQuality.FANCY : SodiumGameOptions.DefaultGraphicsQuality.FANCY))
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Clouds Quality")
                        .setTooltip("Controls the quality of rendered clouds in the sky.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.values()))
                        .setBinding((opts, value) -> opts.quality.cloudQuality = value, opts -> opts.quality.cloudQuality)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Leaves Quality")
                        .setTooltip("Controls the quality of leaf blocks rendered in the world.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.values()))
                        .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Weather Quality")
                        .setTooltip("Controls the quality of rain and snow effects.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.values()))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Translucency Quality")
                        .setTooltip("Controls the quality of translucent block effects.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.values()))
                        .setBinding((opts, value) -> opts.quality.translucentBlockQuality = value, opts -> opts.quality.translucentBlockQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Fog")
                        .setTooltip("If enabled, a fog effect will be used for terrain in the distance.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableFog = value, opts -> opts.quality.enableFog)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Vignette")
                        .setTooltip("If enabled, a vignette effect will be rendered on the player's view. This is very unlikely to make a difference " +
                                "to frame rates unless you are fill-rate limited.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Mipmap Levels")
                        .setTooltip("Controls the number of mipmaps which will be used for block model textures. Higher values provide better rendering of blocks " +
                                "in the distance, but may adversely affect performance with many animated textures.")
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage("Quality", ImmutableList.copyOf(groups));
    }

    public static OptionPage performance() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Fast Chunk Setup")
                        .setTooltip("If enabled, Vertex Array Objects will be used in chunk rendering to avoid needing to setup array pointers every chunk render. " +
                                "Requires OpenGL 3.0+ or support for the ARB_vertex_array_object extension.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useVAOs = value, opts -> opts.performance.useVAOs)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(GlVertexArray.isSupported())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Large Chunk Buffers")
                        .setTooltip("If enabled, chunks will be batched into larger vertex buffers to avoid expensive buffer switches while rendering chunks. " +
                                "This can provide a huge boost at high render distances when CPU-bound." +
                                "Requires OpenGL 3.1+ or support for the ARB_copy_buffer extension.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useLargeBuffers = value, opts -> opts.performance.useLargeBuffers)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .setEnabled(false)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Fog Chunk Occlusion")
                        .setTooltip("If enabled, additional chunk culling will be performed through determining whether or not chunks are hidden in the fog. This can " +
                                "eliminate additional chunks that would otherwise be unnecessarily rendered. This option does nothing if fog rendering is disabled.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useFogChunkCulling = value, opts -> opts.performance.useFogChunkCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .build());

        return new OptionPage("Performance", ImmutableList.copyOf(groups));
    }
}
