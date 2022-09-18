import jenkins.model.Jenkins
import hudson.model.Result

/**
 * 빌드 결과 슬랙 알림.
 * 주의사항! 아래 파라미터들이 pipeline parameters에 있으면 안됩니다. 기본값이 바뀝니다.
 * 파라미터 설정.
 * - Job Configure -> This project is parameterized 체크. 추가후 기본값 입력.
 * - Multi-line String Parameter "COMMON_WEB_HOOK_URLS" : 빌드 결과 전송 웹훅 URL.
 * - Multi-line String Parameter "SUCCESS_WEB_HOOK_URLS" : 성공시 전송 웹훅 URL.
 * - Multi-line String Parameter "FAILURE_WEB_HOOK_URLS" : 실패시 전송 웹훅 URL.
 *
 * In-process Script Approval 필수 허용 항목들.
 * - method hudson.model.Cause getShortDescription
 * - method hudson.model.Job getBuildByNumber int
 * - method hudson.model.Run getCauses
 * - method hudson.model.Run getDurationString
 * - method hudson.model.Run getResult
 * - method hudson.model.Run getUrl
 * - method jenkins.model.Jenkins getItemByFullName java.lang.String
 * - method jenkins.model.Jenkins getRootUrl
 * - staticMethod jenkins.model.Jenkins getInstanceOrNull
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
    for (cause in build.getBuildCauses()) {
        if (cause._class.contains("UpstreamCause")) {
            def result = getResult(cause)
            send(result, split(params.COMMON_WEB_HOOK_URLS))
            if (result.status == Result.SUCCESS) {
                send(result, split(params.SUCCESS_WEB_HOOK_URLS))
            } else if (result.status == Result.FAILURE) {
                send(result, split(params.FAILURE_WEB_HOOK_URLS))
            }
        }
    }
}

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

def getStatusColor(status) {
    switch (status) {
        case Result.SUCCESS: return "#2eb886"
        case Result.FAILURE: return "#dc3545"
        default: return "#ffc107"
    }
}

def escapeSpecialLetter(str) {
    return str.replaceAll(/(["])/, '\\\\$1')
}

def split(str) {
    return str ? str.split("\n") : []
}
