// Console UI suites: add Jenkins "Secret text" credentials with IDs GOOGLE_EMAIL, GOOGLE_PASSWORD,
// TOTP_SECRET (same IDs as in withCredentials below). The build agent needs Node.js on PATH and
// dependencies installed under console-login/ (e.g. npm ci --prefix console-login) so Java can run export-console-login.js.
pipeline {
    agent any
    environment {
        SELENIUM_GRID_URL = 'http://selenium-hub.netcorein.com:4444/wd/hub'
    }
    tools { maven 'M3' }
    parameters {
        choice(
            name: 'ENV',
            choices: ['Dev', 'ProdANZ', 'ProdGANZ', 'ProdAPAC', 'ProdGCP', 'ProdUK', 'ProdUS'],
            description: 'Select Region'
        )
        string(
            name: 'SUITE_FILE',
            defaultValue: 'src/test/resources/testNG/MerchandizingTestcases.xml',
            description: 'Suite XML to run'
        )
        booleanParam(
            name: 'TRIGGER_NEXT',
            defaultValue: false,
            description: 'Trigger downstream jobs automatically'
        )
    }
    stages {
        stage('Build') { steps { sh 'mvn clean compile' } }
        stage('Grid Health Check') {
            steps {
                script {
                    echo "Checking Selenium Grid health..."
                    def statusOutput = sh(script: "curl -s --connect-timeout 10 --max-time 30 ${SELENIUM_GRID_URL}/status | head -20", returnStdout: true).trim()
                    if (statusOutput.contains('"ready": true')) echo "✅ Selenium Grid is accessible" else echo "⚠️ Grid may not be ready"
                    def chromeCheck = sh(script: "curl -s --connect-timeout 10 --max-time 30 ${SELENIUM_GRID_URL}/grid/api/hub | grep -o '\"browserName\":\"chrome\"' | wc -l", returnStdout: true).trim()
                    if (chromeCheck != "0") echo "✅ Chrome available on Grid" else echo "⚠️ No Chrome found"
                }
            }
        }
        stage('Test') {
            steps {
                sh 'rm -rf Extent_Report Extent_Report.zip || true'
                script {
                    def suiteToRun = params.SUITE_FILE
                    if (env.JOB_NAME == 'selfServe_manage_testcases') {
                        suiteToRun = 'src/test/resources/testNG/ManageTestcases.xml'
                    } else if (env.JOB_NAME == 'SS_BulkUploadTest') {
                        suiteToRun = 'src/test/resources/testNG/BulkUploadTest.xml'
                    }
                    // Console UI suites use Node/Puppeteer Google login — inject secrets from Jenkins (not from repo .env).
                    def isConsole = (
                        suiteToRun.toLowerCase().contains('migration') ||
                        suiteToRun.toLowerCase().contains('console') ||
                        suiteToRun.toLowerCase().contains('merchandizing') ||
                        suiteToRun == 'testng-console.xml' ||
                        suiteToRun == 'testng-console-ui.xml' ||
                        suiteToRun.endsWith('/testng-console.xml') ||
                        suiteToRun.endsWith('/testng-console-ui.xml')
                    )
                    echo "Running tests on ENV: ${params.ENV} | Suite: ${suiteToRun} | Console Google login: ${isConsole}"
                    def mvnCmd = "mvn clean test -P${params.ENV} -Denv.profile=${params.ENV} -DhubUrl=${env.SELENIUM_GRID_URL} -DsuiteXmlFile=${suiteToRun} -Dlistener=core.reporting.ExtentTestNGITestListener,core.AnnotationTransformer"
                    int mvnStatus
                    if (isConsole) {
                        withCredentials([
                            string(credentialsId: 'GOOGLE_EMAIL', variable: 'GOOGLE_EMAIL'),
                            string(credentialsId: 'GOOGLE_PASSWORD', variable: 'GOOGLE_PASSWORD'),
                            string(credentialsId: 'TOTP_SECRET', variable: 'TOTP_SECRET')
                        ]) {
                            mvnStatus = sh(
                                script: """
                                    export HEADLESS=true
                                    export USE_CONSOLE_GOOGLE_LOGIN=true
                                    ${mvnCmd}
                                """,
                                returnStatus: true
                            )
                        }
                    } else {
                        mvnStatus = sh(script: mvnCmd, returnStatus: true)
                    }
                    if (mvnStatus != 0) currentBuild.result = 'FAILURE'
                    def sourcePath = fileExists('extent.html') ? 'extent.html' :
                                     fileExists('test-output/ExtentReport.html') ? 'test-output/ExtentReport.html' : ''
                    env.EXTENT_REPORT_PATH = 'Extent_Report/index.html'
                    if (sourcePath) {
                        sh "mkdir -p Extent_Report && cp \"${sourcePath}\" Extent_Report/index.html && zip -r Extent_Report.zip Extent_Report || true"
                    } else {
                        sh "mkdir -p Extent_Report && echo '<html><body><h2>No Extent report produced for build #${env.BUILD_NUMBER}</h2></body></html>' > Extent_Report/index.html && zip -r Extent_Report.zip Extent_Report || true"
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(
                channel: '#qa-automation-reports',
                color: 'good',
                tokenCredentialId: 'slackID',
                teamDomain: 'unbxd',
                botUser: true,
                message: """🎉 *${env.JOB_NAME.toUpperCase()} SUCCESS* 🎉
🏗️ **Job**: ${env.JOB_NAME}
🔢 **Build**: #${env.BUILD_NUMBER}
⏰ **Duration**: ${currentBuild.durationString}
🌍 **Environment**: ${params.ENV}
🌿 **Branch**: ${env.BRANCH_NAME ?: 'origin/main'}
🚀 **Build Cause**: 🤖 Manual Trigger
🔗 <${env.BUILD_URL}|View Build>
📊 <${env.BUILD_URL}Extent_Report/|View Reports>
📦 <${env.BUILD_URL}artifact/${env.EXTENT_REPORT_PATH}|View Extent HTML Report Artifact>
📦 <${env.BUILD_URL}artifact/Extent_Report.zip|Download Full Extent Report ZIP>
📋 <${env.BUILD_URL}testngreports/|View TestNG Report>"""
            )
        }
        failure {
            slackSend(
                channel: '#qa-automation-reports',
                color: 'danger',
                tokenCredentialId: 'slackID',
                teamDomain: 'unbxd',
                botUser: true,
                message: """💥 *${env.JOB_NAME.toUpperCase()} FAILED* 💥
🏗️ **Job**: ${env.JOB_NAME}
🔢 **Build**: #${env.BUILD_NUMBER}
⏰ **Duration**: ${currentBuild.durationString}
🌍 **Environment**: ${params.ENV}
🌿 **Branch**: ${env.BRANCH_NAME ?: 'origin/main'}
🚀 **Build Cause**: 🤖 Manual Trigger
🔗 <${env.BUILD_URL}|View Build>
📊 <${env.BUILD_URL}Extent_Report/|View Reports>
📦 <${env.BUILD_URL}artifact/${env.EXTENT_REPORT_PATH}|View Extent HTML Report Artifact>
📦 <${env.BUILD_URL}artifact/Extent_Report.zip|Download Full Extent Report ZIP>
📋 <${env.BUILD_URL}testngreports/|View TestNG Report>"""
            )
        }
        aborted {
            slackSend(
                channel: '#qa-automation-reports',
                color: 'warning',
                tokenCredentialId: 'slackID',
                teamDomain: 'unbxd',
                botUser: true,
                message: """⏸️ *${env.JOB_NAME.toUpperCase()} ABORTED* ⏸️
🏗️ **Job**: ${env.JOB_NAME}
🔢 **Build**: #${env.BUILD_NUMBER}
🌍 **Environment**: ${params.ENV}
🌿 **Branch**: ${env.BRANCH_NAME ?: 'origin/main'}
🚀 **Build Cause**: 🤖 Manual Trigger
🔗 <${env.BUILD_URL}|View Build>
📊 <${env.BUILD_URL}Extent_Report/|View Reports>
📦 <${env.BUILD_URL}artifact/${env.EXTENT_REPORT_PATH}|View Extent HTML Report Artifact>
📦 <${env.BUILD_URL}artifact/Extent_Report.zip|Download Full Extent Report ZIP>
📋 <${env.BUILD_URL}testngreports/|View TestNG Report>"""
            )
        }
        always {
            junit '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'Extent_Report/index.html,Extent_Report.zip', onlyIfSuccessful: false
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'Extent_Report',
                reportFiles: 'index.html',
                reportName: 'Extent Report'
            ])
            
            // -----------------------------
            // Downstream Jobs Trigger Logic (runs regardless of build result)
            // -----------------------------
            script {
                if (params.TRIGGER_NEXT) {
                    try {
                        // Chain 1: merchandisingNew -> FTU_SmokeTest
                        if (env.JOB_NAME == 'merchandisingNew') {
                            build job: 'FTU_SmokeTest',
                                  parameters: [string(name: 'ENV', value: params.ENV), booleanParam(name: 'TRIGGER_NEXT', value: false)],
                                  wait: false,
                                  propagate: false
                        }
                        // Chain 2: selfServe_manage_testcases -> SS_BulkUploadTest -> website-preview-automation
                        else if (env.JOB_NAME == 'selfServe_manage_testcases') {
                            build job: 'SS_BulkUploadTest',
                                  parameters: [
                                      string(name: 'ENV', value: params.ENV),
                                      string(name: 'SUITE_FILE', value: 'src/test/resources/testNG/BulkUploadTest.xml'),
                                      booleanParam(name: 'TRIGGER_NEXT', value: true)
                                  ],
                                  wait: false,
                                  propagate: false
                        }
                        else if (env.JOB_NAME == 'SS_BulkUploadTest') {
                            build job: 'website-preview-automation',
                                  parameters: [string(name: 'ENV', value: params.ENV), booleanParam(name: 'TRIGGER_NEXT', value: false)],
                                  wait: false,
                                  propagate: false
                        }
                    } catch (err) {
                        echo "Downstream trigger error: ${err}"
                    }
                }
            }
        }
    }
}