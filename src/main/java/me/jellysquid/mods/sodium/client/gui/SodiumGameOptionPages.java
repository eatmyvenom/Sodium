package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
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
                        .setName("Weather Quality")
                        .setTooltip("Controls the quality of rain and snow effects.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.values()))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.LightingQuality.class, sodiumOpts)
                        .setName("Smooth Lighting")
                        .setTooltip("Controls the quality of smooth lighting effects.\n" +
                                "\nOff - No smooth lighting." +
                                "\nLow - Smooth block lighting only." +
                                "\nHigh - Smooth block and entity lighting.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.LightingQuality.values()))
                        .setBinding((opts, value) -> opts.quality.smoothLighting = value, opts -> opts.quality.smoothLighting)
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
                        .setName("Advanced Entity Culling")
                        .setTooltip("If enabled, a secondary culling pass will be performed before attempting to render an entity. This additional pass " +
                                "takes into account the current set of visible chunks and removes entities which are not in any visible chunks.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.useAdvancedEntityCulling = value, opts -> opts.performance.useAdvancedEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Consolidate Render Layers")
                        .setTooltip("If enabled, render layers will be consolidated where possible, reducing the number of render passes that have to be performed.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.useRenderLayerConsolidation = value, opts -> opts.performance.useRenderLayerConsolidation)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Fast Chunk Setup")
                        .setTooltip("If enabled, Vertex Array Objects will be used in chunk rendering to avoid needing to setup array pointers every chunk render. " +
                                "\n\nRequires OpenGL 3.0+ or support for the ARB_vertex_array_object extension.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useVertexArrays = value, opts -> opts.performance.useVertexArrays)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(GlVertexArray.isSupported())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Immutable Storage")
                        .setTooltip("If enabled, immutable storage objects will be used for storing chunk meshes. This can improve performance by giving the driver, " +
                                "more information about how the data will be used, in turn allowing it to apply additional optimizations." +
                                "\n\nRequires OpenGL 4.4+ or support for the ARB_buffer_storage extension.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useImmutableStorage = value, opts -> opts.performance.useImmutableStorage)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(GlImmutableBuffer.isSupported())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Large Chunk Buffers")
                        .setTooltip("If enabled, chunks will be batched into larger vertex buffers to avoid expensive buffer switches while rendering chunks. " +
                                "This can provide a huge boost at high render distances when CPU-bound." +
                                "\n\nRequires OpenGL 3.1+ or support for the ARB_copy_buffer extension.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useLargeBuffers = value, opts -> opts.performance.useLargeBuffers)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .setEnabled(false)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Animate Only Visible Textures")
                        .setTooltip("If enabled, only animated textures determined to be visible will be updated. This can provide a significant boost to frame " +
                                "rates on some hardware. If you experience issues with some textures not being animated, disable this option.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Particle Culling")
                        .setTooltip("If enabled, only particles which are determined to be visible will be rendered. This can provide a significant improvement " +
                                "to frame rates when many particles are nearby.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useParticleCulling = value, opts -> opts.performance.useParticleCulling)
                        .build()
                )
                .build());

        return new OptionPage("Performance", ImmutableList.copyOf(groups));
    }
}
