# Atrial Mapper

A real-time 3D cardiac surface mapping demo built with Java and OpenGL (LWJGL).

---

## What it does

Renders a 3D heart mesh with a live animated electrical activation heatmap. A background
thread continuously simulates electrode data and streams it to the render thread, which
maps the values to a blue→green→red color scale across the heart surface.

- Orbit camera — left-click drag to rotate, scroll to zoom
- Press `W` to toggle wireframe
- Color legend bar on the right showing the voltage scale
- FPS counter in the window title

---

## Architecture

| Class | Responsibility |
|---|---|
| `Main` | Game loop, projection matrix, key input |
| `WindowManager` | GLFW window lifecycle, OpenGL capability check |
| `ShaderCompiler` | Shared GLSL compile/link logic used by all shader pipelines |
| `Shader` | Surface shader — compiles GLSL, uploads uniforms to GPU |
| `Mesh` | GPU vertex buffers, sphere generation, STL mesh support |
| `Camera` | Spherical orbit camera, mouse/scroll callbacks |
| `ElectrodeDataStream` | Background producer thread, `AtomicReference` handoff |
| `DataSimulator` | SA-node activation and reentrant wave simulation |
| `StlLoader` | ASCII and binary STL parser, normalizes mesh to unit scale |
| `MeshData` | Data transfer object between `StlLoader` and `Mesh` |
| `MatrixMath` | 4×4 matrix utilities (projection, rotation, multiply) |
| `ColorLegend` | Responsive 2D voltage scale overlay with tick labels |
| `FontRenderer` | TrueType font atlas rendering via STB |

---

## Concurrency

```
Render thread:   [poll latest] → [upload to GPU] → [draw] → repeat
Producer thread: [simulate]    → [publish]        → [sleep 16ms] → repeat
```

`AtomicReference<float[]>` provides lock-free handoff — no blocking, no locks.

---

## Stack

- Java 21 + Gradle 8.8
- LWJGL 3.3.3 (OpenGL 3.3 Core, GLFW)

---

## Run

```bash
./gradlew run
```

> **IntelliJ:** add `-XstartOnFirstThread` to VM options in your run configuration (required for GLFW on macOS).
