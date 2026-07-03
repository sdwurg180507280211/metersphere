# ONLYOFFICE Excel POC

This POC runs an ONLYOFFICE Document Server and a small document host that
implements the complete open, edit, callback, and save flow for an `.xlsx`
file.

## Start

```bash
cd .kiro/specs/onlyoffice-excel-poc
docker compose up -d
```

Open <http://localhost:8090>. The Document Server status page is available at
<http://localhost:8089>.

## Verify

```bash
curl -fsS http://localhost:8089/healthcheck
curl -fsS http://localhost:8090/health
curl -fsS http://localhost:8090/state
docker compose ps
```

The test host stores the edited workbook in the `poc_files` Docker volume.
ONLYOFFICE sends status `2` when the last editor closes and status `6` after a
forced save. Both statuses replace the stored workbook atomically.

## Stop or reset

```bash
# Stop and retain the edited workbook.
docker compose down

# Remove containers and all POC data.
docker compose down -v
```

This configuration is for local testing only. Production deployment must use a
random secret, HTTPS, restricted network access, persistent backup, and a
licensed edition sized for the expected concurrency.
