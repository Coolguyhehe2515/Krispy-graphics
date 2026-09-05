# Krispy Graphics

A native RenderDragon shader for Minecraft Bedrock Edition, built from scratch (not a fork) for the current Android RenderDragon pipeline.

Krispy Graphics is heavily inspired by [Newb Shader] (https://github.com/devendrn/newb-x-mcbe) by devendrn.

# What's in this shader

- Waving plants and foliage
- Custom day/night lighting driven by sky/block light and fog color
- Procedural noise-based clouds with parallax-style depth
- Tunable settings — no need to touch shader code for common adjustments

# Installing

Stock Minecraft can't load custom "material.bin" files, so you'll need a loader app first.

1. Install MB Loader from the Play Store: [Download here] (https://play.google.com/store/apps/details?id=io.github.bambosan.mbloader&hl=en)
2. Download the latest build from the "Actions" (../../actions)
3. Import the pack into MB Loader's Minecraft install.
4. Enable it under Global Resources or your world's resource pack list.

Building it yourself

This repo builds automatically via GitHub Actions on every push to "main" — no local setup required. If you want to build manually:

1. Install [Lazurite] (https://veka0.github.io/lazurite/):
   
   "pip install lazurite"

2. Grab a "shaderc" binary (Linux example: [devendrn/newb-shader releases] (https://github.com/devendrn/newb-shader/releases))

3. Run:

lazurite build ./project -p android --shaderc ./shaderc -o ./build/materials

See ".github/workflows/build-shader.yml" for the exact build steps used in CI.

# Making your own variant

Want to tweak Krispy Graphics into your own thing? Here's the easiest path:

1. Fork the repo

Click Fork at the top of this page. This gives you your own copy with full edit access, and GitHub Actions will build it automatically on push — no extra setup needed.

2. Start with "settings.h"

Most visual tuning lives in one place: ""krispy-graphics/settings.h"" (./krispy-graphics/settings.h).

This file controls things like:

- Wave speed, amplitude, and frequency for plants
- Sky brightness, torch color, and torch intensity
- Cloud density, scale, and rim highlight strength

Change these values, push, and the next Actions build reflects your edits — no shader code required for most stylistic changes.

3. Rename your fork (optional but recommended)

If you're planning to publish your variant, update:

- "pack/manifest.json" — change ""name"" and ""description"" under "header", and generate new UUIDs so your pack doesn't conflict with other installations.
- This README — swap in your own name and credits while keeping the Newb Shader attribution intact.

4. Going further — editing shader logic

If you want to change actual rendering behavior, the relevant files are:

- "project/RenderChunk/shaders/vertex.sc" / "fragment.sc" — terrain, lighting, plant waving
- "project/Clouds/shaders/vertex.sc" / "fragment.sc" — cloud rendering
- "project/Sky/shaders/vertex.sc" / "fragment.sc" — sky gradient

These are BGFX shader (".sc") files compiled via Lazurite + shaderc, not raw GLSL — see "Lazurite's docs" (https://veka0.github.io/lazurite/) if you're new to this format.

5. Custom textures or pack icon

Drop any custom textures, sounds, or a "pack_icon.png" into the "assets/" folder at the repo root.

Everything in there gets copied straight into the final pack during the build — no ".yml" changes needed.

Credits

- [Newb Shader] (https://github.com/devendrn/newb-x-mcbe) by devendrn — primary inspiration
- [Lazurite] (https://github.com/veka0/lazurite) by veka0 — the shader build tool this project relies on
- [mcbe-shader-codebase] (https://github.com/veka0/mcbe-shader-codebase) by veka0 — restored vanilla material reference
- Vibrant visual like cloud by minmin

License

MIT License

Copyright (c) 2026 Coolguyhehe2515

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
