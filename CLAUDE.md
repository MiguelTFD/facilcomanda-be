# CLAUDE.md — facilcomanda-backend

Este repo se gobierna por la constitución SDD ubicada un nivel arriba:

- `../AGENTS.md` — reglas inviolables del agente (léelo completo antes de tocar código).
- `../spec/constitution/` — misión, stack congelado y roadmap.
- `../spec/features/` — specs; sin spec aprobada no se escribe código.

Recordatorios específicos de este repo: Java 17 + Spring Boot 3.2.5, arquitectura Controller → Service → Repository, multi-tenancy por `organizationId` (patrón `...AndOrganizationId` + filtro Hibernate `tenantFilter`) obligatorio en toda consulta. No tocar `security/` sin spec aprobada. Excepciones de dominio, no `RuntimeException` genérica. Verificar con `./mvnw verify` antes de declarar terminada cualquier tarea.
