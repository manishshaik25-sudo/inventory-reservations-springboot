# Publish to GitHub (Windows / Git Bash / PowerShell)

1. Extract the ZIP to a folder such as `C:\Projects\manhattan-inventory-reservations`.
2. Install Java 21, Maven and Docker Desktop; open Docker Desktop.
3. In a terminal in the extracted project directory run:
   ```bash
   docker compose up -d
   mvn test
   mvn spring-boot:run
   ```
   Check test results. If anything fails, fix it before claiming the demonstration is tested.
4. On github.com select **New repository**. Name it `inventory-reservations-springboot`. Choose **Public**, and leave the GitHub-side README and gitignore checkboxes unchecked.
5. Run these commands **inside the extracted project directory** (substitute your GitHub username):
   ```bash
   git init
   git add .
   git commit -m "Build retry-safe inventory reservation demo"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/inventory-reservations-springboot.git
   git push -u origin main
   ```
6. Open `https://github.com/YOUR_USERNAME/inventory-reservations-springboot` and verify that README.md and `src/` are visible. Take a screenshot of `mvn test` with passing tests; do not fabricate test results.
7. In GitHub, open your **Profile** → **Customize your pins** and pin this repository beside LogiMind.
8. In GitHub repository **About**, add: `Java/Spring Boot + MySQL demo of idempotent inventory reservations and optimistic concurrency with JUnit/Testcontainers tests.`
9. Replace `[link]` in the outreach email with the **actual public repository URL** only after the repo is published and tests are verified.

**Notes:** `.gitignore` excludes compiled output and `.env`. Docker configuration includes local-only example credentials; never reuse them in a real deployment. No connection to your GitHub account is established by this ZIP; the push commands require your local Git authentication.
