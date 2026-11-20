# Todo API

A small Java 17 and Spring Boot REST API. Todos are kept in memory, so they reset when the application stops.

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

The pipeline sets Maven's `revision` property to `0.0.1-<BUILD_NUMBER>-<SHORT_SHA>`. It prints that value in the console and produces `target/todo-api-<BUILD_NUMBER>-<SHORT_SHA>.jar`. On a successful build, Jenkins archives that JAR with fingerprinting enabled and displays it in the build's **Artifacts** section. Local Maven builds continue to use `0.0.1-SNAPSHOT`.

### Screenshots to capture for the assignment

1. **Git history:** terminal output from `git branch -a` and `git log --oneline --graph --all`, showing `main`, `feature/health-endpoint`, and the merge commit.
2. **Jenkins SCM and trigger:** job configuration showing the GitHub repository URL, `*/main`, `Jenkinsfile`, and Poll SCM schedule (`H/5 * * * *`) or the configured webhook trigger.
3. **Successful console output:** completed build log showing checkout, the version line, tests, and packaging succeeded.
4. **Archived artifact:** the successful build's **Artifacts** view showing the versioned JAR.
5. **Build-version enhancement:** console output and/or artifact listing showing the build number and short commit SHA in the version and JAR name.

## Git workflow verification

```sh
git branch -a
git log --oneline --graph --all
git status
```
