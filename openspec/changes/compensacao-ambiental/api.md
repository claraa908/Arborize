# Contrato HTTP — Compensação Ambiental

Todas as rotas exigem autenticação e perfil `FUNCIONARIO_AMMA` ou `ADMINISTRADOR_AMMA`. Os erros da capacidade usam `application/problem+json`; falhas de validação retornam `400`, ausência de registro `404`, falta de autenticação `401`, perfil não autorizado `403`, arquivo acima do limite `413` e falha MinIO `503`.

| Método e rota | Entrada | Saída |
| --- | --- | --- |
| `GET /api/termos-compensacao` | — | Lista de termos e recibos/metadados |
| `POST /api/termos-compensacao` | Multipart: `dados` (JSON `TermoCompensacaoRequestDTO`) e `arquivo` (PDF até 10 MiB) | `201` e `CompensacaoResponseDTO` |
| `GET /api/termos-compensacao/{id}` | — | `CompensacaoResponseDTO` |
| `PUT /api/termos-compensacao/{id}` | JSON `TermoCompensacaoRequestDTO` | `CompensacaoResponseDTO` |
| `PATCH /api/termos-compensacao/{id}/status` | JSON `{ "status": "EM_ABERTO" \| "PARCIALMENTE_DOADO" \| "TOTALMENTE_DOADO" }` | Atualiza estado e retorna `CompensacaoResponseDTO` |
| `POST /api/termos-compensacao/{id}/entregas` | Multipart: `quantidade` (inteiro positivo) e `recibo` (PDF/JPG/PNG até 2 MiB) | Registra parcela, recalcula estado pela soma e retorna `CompensacaoResponseDTO` |
| `GET /api/termos-compensacao/{id}/documento` | — | PDF privado do termo (`no-store`, attachment) |
| `GET /api/termos-compensacao/{id}/entregas/{entregaId}/documento` | — | Recibo privado (`no-store`, attachment) |

`TermoCompensacaoRequestDTO` exige razão social (máx. 180), CNPJ com 14 dígitos, e quantitativo acordado inteiro positivo; número do termo (máx. 60), data de assinatura e prazo são opcionais. O backend não expõe chaves de objetos MinIO. Cada entrega é um registro independente; nenhuma substitui recibos prévios.
