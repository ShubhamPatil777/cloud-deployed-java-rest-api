# CI/CD

This project separates build verification from deployment.

| Workflow | File | When it runs |
| --- | --- | --- |
| CI | `.github/workflows/ci-cd.yml` | Push to `main`, and pull requests that target `main` |
| Deploy | `.github/workflows/deploy.yml` | Manual `workflow_dispatch` only |

Neither workflow has been executed on GitHub in this workspace. The files are ready to run after the repository is pushed. Local Maven verification is separate and is recorded in the project README after `mvn verify`.

## CI workflow

`ci-cd.yml` has two jobs:

1. **Test and package** checks out the repository, installs Temurin JDK 21, caches Maven dependencies through `actions/setup-java`, and runs `./mvnw -B -ntp verify`. That command compiles the code, runs the unit and integration tests, and packages the executable JAR. A test failure fails the job.
2. **Build Docker image** starts only after the Maven job succeeds. It runs `docker build` and tags the image `cloud-task-api:<commit sha>`. The image is not pushed to a registry and is not deployed.

The workflow requests `contents: read` only. It does not use secrets.

GitHub Actions marks the workflow failed when either job exits non-zero, and successful when both jobs pass. No extra status-reporting step is required.

## Deploy workflow

`deploy.yml` is inactive until someone runs it from the Actions tab. It expects these repository secrets and does not contain their values:

| Secret | Purpose |
| --- | --- |
| `VM_HOST` | Ubuntu VM address |
| `VM_USER` | SSH user that can run Docker |
| `VM_SSH_KEY` | Private key for that user |
| `VM_APP_DIR` | Absolute path of the clone on the VM, for example `/opt/cloud-task-api` |

The job checks that each secret is present, opens an SSH session, fast-forwards the clone, and runs `docker compose up --build -d`. The private key is written to the runner with mode `600` and deleted before the job finishes. The workflow does not print secret values.

Add the secrets under **Settings → Secrets and variables → Actions**. Put the matching public key in the VM user's `~/.ssh/authorized_keys`. Use a GitHub Environment named `production` if you want a manual approval gate; the job already targets that environment. Create the environment before the first run, or remove the `environment: production` line if you do not want one.

The VM must already have Docker Engine, the Compose plugin, and a clone of this repository. Those steps are in [DEPLOYMENT.md](DEPLOYMENT.md). `scripts/deploy.sh` performs the same update on the VM when you are logged in there.

## What this pipeline does not do

- It does not provision a cloud VM.
- It does not push the image to Docker Hub or another registry.
- It does not store SSH keys, tokens, or database passwords in git.
