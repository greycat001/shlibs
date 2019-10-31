#!/usr/bin/groovy

package com.cal;
import jenkins.model.*
import groovy.transform.Field

@Field def globalEnvVarsPath = "path to global properties file"
@Field def localEnvVarsPath = "path to local(custom) properties file"
@Field def pomFile = "pom.xml"

def artifactory
def rtMaven
def globalEnv

def Sources_Checkout(node_name) {
	stage ("Sources Checkout") {
	}
	node ("${node_name}") {
		cleanWs()
		checkout scm
	}
}

return this
