# Instrucciones para Codex

Leé `CLAUDE.md`, `SDD.md`, `tareas.md`, `docs/roadmap.md`, el plan técnico
`docs/briefs/android-client-v3.md` y el norte de producto
`docs/briefs/daily-discovery-v1.md` antes de trabajar.

Las invariantes de `CLAUDE.md` también aplican a Codex. No usar datos reales como
fixtures, no editar a mano `contract/` y no declarar implementadas capacidades
que sólo están propuestas en los briefs. Las preguntas abiertas se resuelven con
el owner antes de implementar lo que dependa de ellas.

## UI y primera entrega mobile

Seguir la decisión del owner del 2026-10-04 en CLAUDE.md: Compose + MVVM;
si hay XML/Views, View Binding. Leer `docs/briefs/mobile-pairing-v1.md` para el
orden de emparejamiento, pruebas de sync y QA aislado. No usar dumps privados
como fixtures ni confundir el placeholder compilable con una app funcional.
