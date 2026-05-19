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
| `Main` | Window, game loop, projection matrix, key input |
| `Shader` | Compiles GLSL shaders, uploads uniforms to GPU |
| `Mesh` | GPU vertex buffers, sphere generation, STL loading |
| `Camera` | Spherical orbit camera, mouse/scroll callbacks |
| `ElectrodeDataStream` | Background producer thread, `AtomicReference` handoff |
| `DataSimulator` | Spatially accurate wave simulation using vertex positions |
| `StlLoader` | ASCII and binary STL parser, normalizes mesh to unit scale |
| `ColorLegend` | 2D screen-space gradient overlay, independent shader |

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
