# Todo API

## Requirements

- Java 17
- Maven 3.6.3 or newer

## Run locally

```sh
mvn spring-boot:run
```

The server listens on `http://localhost:8080`.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/todos` | Return all todos |
| `POST` | `/todos` | Create a todo from `{"title":"..."}` |
| `GET` | `/health` | Return `{"status":"UP"}` |

Example:

```sh
curl -X POST http://localhost:8080/todos -H "Content-Type: application/json" -d '{"title":"Learn Spring Boot"}'
curl http://localhost:8080/todos
curl http://localhost:8080/health
```

Blank todo titles are rejected with HTTP 400. A successful create returns HTTP 201 and a todo with an automatically assigned ID and `completed: false`.

## Verify and package locally

```sh
mvn test
mvn clean package
```

The tests cover health, creating/listing todos, and rejecting a blank title.

## Ansible deployment on Linux or WSL

The `ansible/` directory has an Ansible Galaxy-compatible `todo_api` role, a localhost inventory, and a playbook. It installs the Java 17 runtime for Debian- or Red Hat-family Linux, verifies the Java version, copies the built JAR to `/opt/todo-api/app.jar`, and manages a systemd service. The service runs as the restricted `todo-api` account. Set the listening port with the `app_port` variable (default `8080`). Re-running the playbook does not restart the service unless the JAR or systemd unit changes.

### Requirements and privilege

- Run Ansible from Linux or WSL with Ansible installed; the localhost inventory uses `/usr/bin/python3`.
- The playbook uses `become: true` to install Java and manage `/opt` and `/etc/systemd/system`. Run it as a user allowed to use `sudo`; pass `--ask-become-pass` if sudo needs a password. A root shell does not need the prompt.
- WSL must use systemd as PID 1. On supported WSL versions, enable it inside the distro by adding this to `/etc/wsl.conf`:

  ```ini
  [boot]
  systemd=true
  ```

  Then, from Windows, run `wsl --shutdown`, reopen the distro, and confirm `ps -p 1 -o comm=` reports `systemd`. Ansible's systemd service tasks will not work in a WSL distro that is not running systemd.
- Build the Maven JAR before deploying. The default role source expects `target/todo-api-0.0.1-SNAPSHOT.jar` in the project root.

### Install and deploy

On Ubuntu/Debian WSL, install Ansible, Maven, and the Java 17 JDK needed to build locally. The playbook separately installs/checks the Java 17 runtime on the deployment host:

```sh
sudo apt update
sudo apt install -y ansible maven openjdk-17-jdk
```

From the project root in Linux/WSL:

```sh
mvn clean package
ansible-playbook -i ansible/inventory.ini ansible/playbook.yml --ask-become-pass -e app_port=8080
systemctl status todo-api --no-pager
curl http://localhost:8080/health
```

If sudo is configured without a password, omit `--ask-become-pass`. To deploy a Jenkins-versioned JAR instead, pass its Linux path explicitly, for example:

```sh
ansible-playbook -i ansible/inventory.ini ansible/playbook.yml --ask-become-pass \
  -e app_jar_source="$PWD/target/todo-api-0.0.1-12-a1b2c3d.jar" -e app_port=8080
```

The `app_port` variable is written as `SERVER_PORT` in the systemd unit. On a successful first deployment, Ansible enables and starts the service; later changes to the JAR or unit notify the restart handler.

## Docker image

The Dockerfile uses a Java 17 runtime image, copies the Maven-built executable JAR, runs it as an unprivileged user, and exposes port 8080. Build the JAR first so Docker has an artifact to copy. On Windows, [install Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/) with the WSL 2 backend, start it, then enable your distro under **Settings → Resources → WSL Integration**. On Linux/WSL, Docker Engine must be installed and running.

```sh
mvn clean package
docker build -t todo-api:local .
docker run --rm -d --name todo-api -p 8080:8080 -e SERVER_PORT=8080 todo-api:local
curl http://localhost:8080/health
docker stop todo-api
```

To use another host port, map it to container port 8080 (for example `-p 8081:8080`) and open `http://localhost:8081/health`. `.dockerignore` excludes source, Git metadata, and unrelated build output while retaining the built Todo API JAR.

## Jenkins CI pipeline

The repository includes a declarative [`Jenkinsfile`](Jenkinsfile). It checks out the job's SCM revision, runs tests, packages the app, publishes JUnit results, and archives the executable JAR. Jenkins logs and the JAR filename show a version containing the Jenkins build number and short Git commit SHA, for example `todo-api-0.0.1-12-a1b2c3d.jar`.

### 1. Configure Jenkins tools

In Jenkins, open **Manage Jenkins → Tools** (called **Global Tool Configuration** on some versions):

1. Under **JDK installations**, add a JDK named exactly `JDK17` and select/install a Java 17 distribution.
2. Under **Maven installations**, add Maven named exactly `Maven3` and select/install Maven 3.6.3 or newer.
3. Save. The Jenkins agent that runs the pipeline must be able to use these installations (or their configured installers must be available).

### 2. Create a Pipeline job connected to Git

1. Select **New Item**, enter a job name such as `todo-api-ci`, choose **Pipeline**, and select **OK**.
2. In **General**, enable **GitHub project** if available and enter `https://github.com/bubksrit/devops-ci-cd-pipeline`.
3. Under **Build Triggers**, select **Poll SCM** and enter `H/5 * * * *` to check for changes approximately every five minutes. Jenkins schedules these checks; unchanged revisions do not start a build.
4. Under **Pipeline**, set **Definition** to **Pipeline script from SCM**.
5. Set **SCM** to **Git**, **Repository URL** to `https://github.com/bubksrit/devops-ci-cd-pipeline`, and **Branch Specifier** to `*/main`.
6. If the repository is private, select a Jenkins credential with repository read access. Do not put a password or token in the URL or in this repository.
7. Set **Script Path** to `Jenkinsfile`, then save and select **Build Now** for the first run.

The Jenkins controller must have network access to GitHub for checkout and SCM polling. The build agent needs the configured JDK and Maven. The pipeline supports both Unix and Windows agents.

### Optional: GitHub webhook trigger

Polling works without exposing Jenkins publicly. For push-triggered builds instead, Jenkins must be reachable by GitHub over HTTPS and have the GitHub integration plugin installed. Add a webhook in the GitHub repository under **Settings → Webhooks**:

- **Payload URL:** `https://<your-public-jenkins-url>/github-webhook/`
- **Content type:** `application/json`
- **Event:** push events

Then enable **GitHub hook trigger for GITScm polling** under the Jenkins job's **Build Triggers** and save. Replace the placeholder with your actual public Jenkins address; a private `localhost` address cannot receive GitHub webhooks.

### Build version and artifact

The pipeline polls SCM every five minutes, sets Maven's `revision` property to `0.0.1-<BUILD_NUMBER>-<SHORT_SHA>`, builds and tests the app, archives the executable JAR, and builds Docker image tags `todo-api:<SHORT_SHA>` and `todo-api:<version>`. The Jenkins agent needs Docker available. Local Maven builds continue to use `0.0.1-SNAPSHOT`.

## Kubernetes deployment

`k8s/todo-api.yaml` defines a three-replica Deployment with startup, liveness, and readiness probes and resource requests/limits, plus a NodePort Service on port `30080`. The startup probe allows the Spring Boot application time to initialize before Kubernetes begins its liveness and readiness checks. Start a Kubernetes cluster such as Minikube, build the image, and make it available to that cluster. With Minikube using the Docker driver:

```sh
mvn clean package
docker build -t todo-api:local .
minikube image load todo-api:local
kubectl apply -f k8s/todo-api.yaml
kubectl rollout status deployment/todo-api
minikube service todo-api --url
```

Use the URL printed by `minikube service` and append `/health` to check the running API. To remove the assignment resources, run `kubectl delete -f k8s/todo-api.yaml`. Jenkins builds SHA and build-version tags for the container image; Kubernetes deployment uses the local `todo-api:local` tag in this manifest, so load that tag into the cluster before applying it. A registry based deployment requires publishing the image and changing the manifest image reference.

## Git workflow verification

```sh
git branch -a
git log --oneline --graph --all
git status
```
