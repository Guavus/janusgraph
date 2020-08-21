@Library('jenkins_lib')_
pipeline {
  agent {label 'slave'}
 
  environment {
    // Define global environment variables in this section

    buildNum = currentBuild.getNumber() ;
    buildType = BRANCH_NAME.split("/").first();
    branchVersion = BRANCH_NAME.split("/").last().toUpperCase();

    ARTIFACT_SRC_JANUSGRAPH_ZIP = './janusgraph-dist/janusgraph-dist-hadoop-2/target'
    ARTIFACT_DEST_JANUSGRAPH_ZIP = 'ggn-archive/raf/janusgraph'

    ARCHIVE_ZIP_PATH_JANUSGRAPH = "./janusgraph-dist/janusgraph-dist-hadoop-2/target/*.zip"

    SLACK_CHANNEL = 'jenkins-cdap-alerts'
    CHECKSTYLE_FILE = 'target/javastyle-result.xml'
    UNIT_RESULT = 'target/surefire-reports/*.xml'
    COBERTURA_REPORT = 'target/site/cobertura/coverage.xml'
    ALLURE_REPORT = 'allure-report/'
    HTML_REPORT = 'index.html'

    SONAR_PATH = './'
  }

    stages 
    {
        stage("Define Release Version") {
            steps {
                script {
                    //Global Lib for Environment Versions Definition
                    versionDefine()
                }
            }
        }

        stage("Initialize variable") {
            steps {
                script {
                    PUSH_JAR = false;
                    
                    if( env.buildType ==~ /(release)/)
                    {
                        PUSH_JAR = false;
                    }
                }
            }
        }

        stage ("Compile, Build and Deploy Janusgraph")
        {
            stages {
                stage("Compile and build Janusgraph") {
                    steps {
                        script {
                            echo "Running Build"

                            sh "mvn clean install -U -Phadoop2 -Pjanusgraph-release -Dgpg.skip=true -DskipTests=true -Drat.skip=true -Dassembly.cfilter.in.dir.suffix=conf -Dskip.solr_examples=true"
                        }
                    }
                }

                stage("Push JAR to Maven Artifactory") {
                    when {
                        expression { PUSH_JAR == true }
                    }
                    steps {
                        script {
                            echo "Pushing JAR to Maven Artifactory"

                            sh "mvn deploy -U -DskipTests=true -Dcheckstyle.skip=true -Drat.skip=true -Drat.ignoreErrors=true -Dfindbugs.skip=true -Dgpg.skip=true -Phadoop2 -Pjanusgraph-release -Dassembly.cfilter.in.dir.suffix=conf -Dskip.solr_examples=true;"
                        }
                    }
                }

                stage("Artifacts Push for Janusgraph"){
                    steps {
                        script {
                            echo ("Janusgraph")
                            //Global Lib for RPM Push
                            //rpm_push(<env.buildType No need to change>, <dist is default pls specify RPM file path, <artifactory target path>) ie.        
                            tar_push(env.buildType, env.ARTIFACT_SRC_JANUSGRAPH_ZIP, env.ARTIFACT_DEST_JANUSGRAPH_ZIP)
                        }
                    }
                }
            }
        }
    }
    
    post {
       always {
            //Global Lib for Reports publishing
            reports_alerts(env.CHECKSTYLE_FILE, env.UNIT_RESULT, env.COBERTURA_REPORT, env.ALLURE_REPORT, env.HTML_REPORT)
 
            //Global Lib for post build actions eg: artifacts archive
            postBuild(env.ARCHIVE_ZIP_PATH_JANUSGRAPH)

            //Global Lib for slack alerts
            slackalert(env.SLACK_CHANNEL)
      }
    }
}