### 3.3.2

_Released 2023 Apr 01_

This build includes the following changes:

#### Bindings

- Added [FMOD](https://www.fmod.com) bindings. (#295)
  * Native binaries are not included, because the license does not permit redistribution. They must be downloaded and deployed separately from LWJGL.
- Added [FreeType](https://freetype.org/) bindings. (#611)
  * The default build includes HarfBuzz and exports its full API.
- Added [HarfBuzz](https://harfbuzz.github.io/) bindings. (#611)
  * The default build works standalone.
  * FreeType interop can be enabled by making HarfBuzz use FreeType's shared library (see `Configuration.HARFBUZZ_LIBRARY_NAME`).
- Added [hwloc](https://www.open-mpi.org/projects/hwloc/) bindings. (#98)
- Added [KTX (Khronos Texture)](https://www.khronos.org/ktx/) bindings. (#660)
- Windows: Added bindings to `dpapi.h`. (#851)
- Assimp: Updated to 5.2.5 (up from 5.2.2)
  * `KHR_draco_mesh_compression` is now supported via the bundled [Draco](https://github.com/google/draco) library. (#773)
- bgfx: Updated to API version 118 (up from 115)
- CUDA: Updated to 12.1.0 (up from 11.5.0)
- glfw: Updated to latest 3.3.8 (up from 3.3.6)
  * Added `GLFW_WAYLAND_LIBDECOR`, `GLFW_WAYLAND_PREFER_LIBDECOR` and `GLFW_WAYLAND_DISABLE_LIBDECOR`.
  * Added `GLFW_CURSOR_CAPTURED`.
  * Added `GLFW_WAYLAND_APP_ID`.
  * Added `GLFW_POSITION_X`, `GLFW_POSITION_Y` and `GLFW_ANY_POSITION`.
- jemalloc: Updated to 5.3.0 (up from 5.2.1)
- libffi: Updated to 3.4.4 (up from 3.4.2)
- liburing: Updated to 2.4-dev (up from 2.1)
- lmdb: Updated to 0.9.30 (up from 0.9.28)
- lz4: Updated to 1.9.4 (up from 1.9.3)
- LLVM/Clang: Updated to 16.0.0 (up from 13.0.1)
- meshoptimizer: Updated to 0.18 (up from 0.17)
- NativeFileDialog: Switched to [Native File Dialog Extended](https://github.com/btzy/nativefiledialog-extended) (#823)
  * This is a fork of the original library with new features and breaking API changes.
- Nuklear: Updated to 4.10.5 (up from 4.9.6)
  * Added the font baking API.
- OpenAL Soft: Updated to 1.23.0 (up from 1.21.1)
  * Added `ALC_SOFT_loopback_bformat` extension.
  * Added `ALC_SOFT_output_mode` extension.
  * Added `ALC_SOFT_reopen_device` extension.
  * Added `AL_SOFT_callback_buffer` extension.
  * Added `AL_SOFT_effect_target` extension.
  * Added `AL_SOFT_events` extension. (#854)
  * Added `AL_SOFT_UHJ` extension.
  * Added `AL_SOFTX_hold_on_disconnect` extension. (#795)
- OpenGL(ES): Added latest extensions.
  * `GL_EXT_framebuffer_blit_layers`
  * `GL_EXT_fragment_shading_rate`
  * `GL_EXT_fragment_shading_rate_attachment`
  * `GL_EXT_fragment_shading_rate_primitive`
  * `GL_EXT_separate_depth_stencil`
  * `GL_EXT_shader_samples_identical`
- OpenVR: Updated to 1.23.7 (up from 1.16.8)
- OpenXR: Updated to 1.0.27 (up from 1.0.22)
- Remotery: Updated to 1.2.1 (up from 1.0.0)
- rpmalloc: Updated to 1.4.4 (up from 1.4.3)
- Shaderc: Updated to 2023.3 (up from 2022.1)
- SPIRV-Cross: Updated to 0.51.0 (up from 0.48.0)
- stb
  * Updated `stb_image` to 2.28 (up from 2.27)
- tinyexr: Updated to 1.0.2 (up from 1.0.1)
- tinyfiledialogs: Updated to 3.9.0 (up from 3.8.8)
- vma: Updated to 3.0.1 (up from 3.0.0-development)
- Vulkan: Updated to 1.3.246 (up from 1.3.206)
  * Includes MoltenVK 1.2.3 (up from 1.1.7)
- Zstd: Updated to 1.5.4 (up from 1.5.2)

#### Improvements

- Linux: x64 shared libraries are now built with GCC 11.2 (up from GCC 7.5)
  * The minimum GLIBC version is now 2.17. (down from 2.27) (#842)
- Windows: Shared libraries are now built with Visual Studio 2022 (up from 2019)
- Core: Added support for JDK 19 (#799)
- Core: Added `MemoryUtil::memByteBuffer(Struct)`. It creates a `ByteBuffer` view of a struct value.
- Core: More `SharedLibraryLoader` improvements. (#790)
  * The default `Configuration.SHARED_LIBRARY_EXTRACT_DIRECTORY` is now `lwjgl_<trimmed_user_name>`.
  * The default `Configuration.SHARED_LIBRARY_EXTRACT_PATH` now includes the CPU architecture. (`<temp_root>/<extract_dir>/<version>/<arch>/`)
  * Added `Configuration.SHARED_LIBRARY_EXTRACT_FORCE`.
- Core: The string returned by `Version::getVersion()` now follows the Java version format (`M.m.r+B` or `M.m.r-snapshot+B`).
- Core: Added `Configuration.DEBUG_MEMORY_ALLOCATOR_FAST`, a dynamic option that dramatically reduces the performance overhead of memory leak detection.
- Core: Reduced the performance overhead of `Configuration.DEBUG_STACK`.
- docs: The LWJGL javadoc is now generated with JDK 19 (up from JDK 10) for improved search functionality.
- The `.sha1` and `.git` files, used for validating LWJGL artifacts, are now stored under the `META-INF` folder.
- glfw: It will now always be patched with the latest [SDL_GameControllerDB](https://github.com/gabomdq/SDL_GameControllerDB) version.
  * Reminder: `glfwUpdateGamepadMappings` can be used to update the mappings at runtime.
- Nuklear: `NK_BUTTON_TRIGGER_ON_RELEASE` is now also defined on Linux & macOS, not only Windows, for consistency.
- OpenXR: Added extension class javadoc.
  * Currently, only the overview section is included.
  * Also added a link that opens the OpenXR specification at the corresponding section for the extension. 
- Remotery: Made it easier to get started with Remotery profiling: (#784)
  * Users can now identify the commit used to build the Remotery bindings in the `Remotery` class javadoc.
  * A script that quickly clones the Remotery repository at that commit is also included.
    The viewer application at `vis/index.html` is guaranteed to be compatible with the LWJGL bindings. 
  * Ported two simple Remotery samples (`modules/samples/src/test/java/org/lwjgl/demo/util/remotery/`).

#### Fixes

- Core: Fixed Java/native library incompatibility detection. (#737)
- Core: Fixed `dlerror` decoding to use UTF-8. (#738)
- Core: Fixed `Version::getVersion()` when LWJGL is in the module path. (#770)
- Core: Fixed handling of unsigned 8/16-bit integer parameters in JNI code. (#858)
- Core: Many debug messages can no longer tear under concurrency. (#825)
- Build: Fixed offline mode with multiple local architectures. (#740)
- NanoSVG: Fixed auto-sizing of `NSVGPath::pts` buffer.
- OpenCL: Fixed initialization on macOS 13. (#861)
- Opus: Fixed `pcm` parameter type of `opus_decode_float` function. (#785)
- Remotery: Fixed `rmtSettings::free` callback signature.
- stb: Fixed `stb_image_resize` flag values. (#857)
- Vulkan: Fixed definition of the `VK_API_VERSION_PATCH` macro. (#743)
- Vulkan: Fixed `EXT_debug_utils` function dispatch. (#755)

#### Breaking Changes

- Core: Introduced additional mangling of `org.lwjgl.system.JNI` method names for 8/16-bit integer parameters. (#858) (S)
- NanoVG: The `freeData` parameters of `nvgCreateFontMem*` functions are now mapped to Java `boolean`. (S)
- NativeFileDialog: Now uses the [btzy/nativefiledialog-extended](https://github.com/btzy/nativefiledialog-extended) API. (S)
- Nuklear: Renamed `NkConvertConfig::null_texture` to `tex_null` to match the change in the native API. (S)

```
(B): binary incompatible change
(S): source incompatible change
```
