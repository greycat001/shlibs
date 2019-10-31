import jenkins.model.*
import groovy.transform.Field

@Field def globalEnvVarsPath = "path to global properties file"
@Field def localEnvVarsPath = "path to local(custom) properties file"
@Field def pomFile = "pom.xml"

def artifactory
def rtMaven
def globalEnv
def url
def credentialsId
def releaseRepo
def snapshotRepo

def call(Map pipelineParams) {
	pipeline{
		agent {
			node { label pipelineParams.node_name }
		}
		stages {
			stage ("Build Initialization") {
				steps {
					script {
						if (fileExists(globalEnvVarsPath)) {
							globalEnv = readProperties interpolate:true, file: "${globalEnvVarsPath}"
							println sh(returnStdout: true, script: "cat ${globalEnvVarsPath}")
						} else {
							log.warning "Global properties file ${globalEnvVarsPath} not found!"
						}
						if (fileExists(localEnvVarsPath)) {
							log.info "Found local build.properties file. Add/Overwrite environment variables."
							load localEnvVarsPath
							println sh(returnStdout: true, script: "cat ${localEnvVarsPath}")
						}
						
						url = globalEnv['ARTIFACTORY_URL']
						credentialsId = globalEnv['ARTIFACTORY_CREDENTIALS_ID']
						artifactory = Artifactory.newServer url: url, credentialsId: credentialsId
						rtMaven = Artifactory.newMavenBuild()
						rtMaven.tool = globalEnv['MAVEN_JENKINS_VERSION_NAME']
						rtMaven.tool = globalEnv['MAVEN_JAVA_BUILD_OPTS']
						rtMaven.tool = globalEnv['ARTIFACTORY_RELEASE_REPO']
						rtMaven.tool = globalEnv['ARTIFACTORY_SNAPSHOT_REPO']
						rtMaven.deployer releaseRepo: releaseRepo, snapshotRepo: snapshotRepo, server: artifactory
					}
				}
			}

			stage ("Run Maven Build") {
				steps {
					script {
						log.info "Running Maven Build on the node ${node_name}."
						def cdsrequestid = "${env.CDSREQUESTID}"
						def jenkinsJobName = "${env.JOB_NAME}"
						def jenkinsJobBuildNumber = "${env.BUILD_NUMBER}"
						def jenkinsBuildTimestamp = "${env.BUILD_TIMESTAMP}"
						def MVNFLAGS = "-DjenkinsJobName=${jenkinsJobName} -DjenkinsJobBuildNumber=${jenkinsJobBuildNumber} -DjenkinsBuildTimestamp=${jenkinsBuildTimestamp} -Dcdsrequestid=${cdsrequestid}"
						def mvnCommand = "globalEnv['MAVEN_FULL_COMMAND'] -s ${WORKSPACE}/globalEnv['MAVEN_SETTINGS_XML_PATH'] globalEnv['MAVEN_FULL_BUILD_PROPERTIES'] globalEnv['MAVEN_FULL_BUILD_PROFILES'] ${MVNFLAGS}"
						ansiCOlor('xtem') {
							// Fails on the next line...
							def buildRes = rtMaven.run pom: pomFile, goals: "mvnCommand"
							artifactory.publishBuildInfo buildRes
						}
					}
				}
			}
		}
	}
}
