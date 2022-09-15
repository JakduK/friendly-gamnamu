import jenkins.model.Jenkins
import hudson.model.Result

/**
 * 슬랙 웹훅URL 설정 방법.
 * Job Configure -> This project is parameterized 체크
 * -> Multi-line String Parameter "COMMON_WEB_HOOKS", "SUCCESS_WEB_HOOKS", "FAILURE_WEB_HOOKS" 추가 -> 기본값 입력
 * 주의사항! pipeline parameter 사용하면 안됩니다.
 */

pipeline {
    agent any

    stages {
        stage("Send") {
            steps {
                script {
                    sendForUpstreamBuilds(currentBuild)
                }
            }
        }
    }
}

def sendForUpstreamBuilds(build) {
    for(cause in build.getBuildCauses()) {
        if (cause._class.contains("UpstreamCause")) {
            def result = getResult(cause)
            send(result, split(params.COMMON_WEB_HOOKS))
            if (result.status == Result.SUCCESS) {
                send(result, split(params.SUCCESS_WEB_HOOKS))
            } else if (result.status == Result.FAILURE) {
                send(result, split(params.FAILURE_WEB_HOOKS))
            }
        }
    }
}

@NonCPS
def getResult(cause) {
    def jenkins = Jenkins.getInstanceOrNull()
    if (jenkins) {
        def upstreamProject = jenkins.getItemByFullName(cause.upstreamProject)
        def upstreamBuild = upstreamProject?.getBuildByNumber(cause.upstreamBuild)
        if (!upstreamProject) {
            return [
                status:Result.FAILURE,
                title:"[${Result.FAILURE}] ${cause.upstreamProject} not found."
            ]
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = "[${Result.FAILURE}] ${cause.upstreamProject} #${cause.upstreamBuild}"
            return [
                status:Result.FAILURE,
                title:title,
                url:url,
                message:"Build not found."
            ]
        } else {
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def title = "[${upstreamBuild.result}] ${upstreamBuild.fullDisplayName}"
            def elapsed = "_${upstreamBuild.durationString} elapsed._"
            def startedBy = "${upstreamBuild.getCauses().collect {"_${it.shortDescription}_"}.join(",\n")}."
            return [
                status: upstreamBuild.result,
                title: title,
                url: url,
                message: [
                    elapsed,
                    startedBy,
                ].findAll {it}.join("\n")
            ]
        }
    } else {
        return [
            status:Result.FAILURE,
            title:"Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent."
        ]
    }
}

def send(result, webHooks) {
    def color = "\"color\": \"${getStatusColor(result.status)}\""
    def title = "\"title\": \"${escapeSpecialLetter(result.title)}\""
    def url = result.url ? "\"title_link\": \"${result.url}\"" : null
    def message = result.message ? "\"text\": \"${escapeSpecialLetter(result.message)}\"" : null
    for (webHook in webHooks) {
        sh """
curl ${webHook} \
-s \
-X POST \
-H 'content-type: application/json' \
-d '{
    \"attachments\": [
        {
            ${
                [
                    color,
                    title,
                    url,
                    message
                ].findAll {it}.join(',')
            }
        }
    ]
}'
"""
    }
}

@NonCPS
def getStatusColor(status) {
    switch (status) {
        case Result.SUCCESS: return "#2eb886"
        case Result.FAILURE: return "#dc3545"
        default: return "#ffc107"
    }
}

@NonCPS
def escapeSpecialLetter(str) {
    return str.replaceAll(/(["])/, '\\\\$1')
}

@NonCPS split(str) {
    return str ? str.split("\n") : []
}