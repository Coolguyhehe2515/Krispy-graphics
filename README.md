# Krispy Graphics

A native RenderDragon shader for Minecraft Bedrock Edition, built from scratch (not a fork) for the current Android RenderDragon pipeline.

Krispy Graphics is heavily inspired by [Newb Shader](https://github.com/devendrn/newb-x-mcbe) by devendrn.

## What's in this shader

- Waving plants and foliage
- Custom day/night lighting driven by sky/block light and fog color
- Procedural noise-based clouds with parallax-style depth
- Tunable settings — no need to touch shader code for common adjustments

## Installing

Stock Minecraft can't load custom `material.bin` files, so you'll need a loader app first.

1. Install MB Loader from the Play Store: [Download here](https://play.google.com/store/apps/details?id=io.github.bambosan.mbloader&hl=en)
2. Download the latest build from the [Actions](../../actions) tab.
3. Import the pack into MB Loader's Minecraft install.
4. Enable it under Global Resources or your world's resource pack list.

## Building it yourself

This repo builds automatically via GitHub Actions on every push to `main` — no local setup required. If you want to build manually:

1. Install [Lazurite](https://veka0.github.io/lazurite/):
