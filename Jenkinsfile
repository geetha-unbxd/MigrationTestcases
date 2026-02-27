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
        stage('Build') {
            steps { sh 'mvn clean compile' }
        }
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
                echo "Running tests on ENV: ${params.ENV} | Suite: ${params.SUITE_FILE}"
                sh 'rm -rf Extent_Report Extent_Report.zip || true'
                script {
                    int mvnStatus = sh(
                        script: "mvn clean test -P${params.ENV} -Denv.profile=${params.ENV} -DhubUrl=${SELENIUM_GRID_URL} -DsuiteXmlFile=${params.SUITE_FILE} -Dlistener=core.reporting.ExtentTestNGITestListener,core.AnnotationTransformer",
                        returnStatus: true
                    )
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
            // Downstream Jobs Trigger Logic
            // -----------------------------
            script {
                if (params.TRIGGER_NEXT) {
                    echo "Auto-trigger enabled. Triggering downstream jobs..."
                    try {
                        // Job 1 -> Job 2 & Job 3
                        if (env.JOB_NAME == 'merchandisingNew') {
                            build job: 'website-preview-automation',
                                  parameters: [string(name: 'ENV', value: params.ENV), booleanParam(name: 'TRIGGER_NEXT', value: false)],
                                  wait: false,
                                  propagate: false
                            build job: 'FTU_Selfserve',
                                  parameters: [string(name: 'ENV', value: params.ENV), booleanParam(name: 'TRIGGER_NEXT', value: false)],
                                  wait: false,
                                  propagate: false
                        }
                        // Job 4 -> Job 5
                        else if (env.JOB_NAME == 'selfServe_manage_testcases') {
                            build job: 'SS_BulkUploadTest',
                                  parameters: [string(name: 'ENV', value: params.ENV), booleanParam(name: 'TRIGGER_NEXT', value: false)],
                                  wait: false,
                                  propagate: false
                        }
                    } catch (err) {
                        echo "Downstream trigger error: ${err}"
                    }
                }

                // -----------------------------
                // Slack Notifications (Always)
                // -----------------------------
                def colorMap = ['SUCCESS':'good','FAILURE':'danger','UNSTABLE':'warning','ABORTED':'warning']
                slackSend(
                    channel: '#qa-automation-reports',
                    color: colorMap[currentBuild.currentResult],
                    message: """🔔 *${env.JOB_NAME.toUpperCase()} ${currentBuild.currentResult}*
🏗️ Job: ${env.JOB_NAME}
🔢 Build: #${env.BUILD_NUMBER}
🌍 ENV: ${params.ENV}
📋 Suite: ${params.SUITE_FILE}
🔗 <${env.BUILD_URL}|View Build>"""
                )
            }
        }
    }
}