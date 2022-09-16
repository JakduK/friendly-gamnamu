import jenkins.model.Jenkins
import hudson.model.Result

/**
 * 텔레그램 bot api token, chat id 설정 방법.
 * Job Configure -> This project is parameterized 체크 -> String Parameter "BOT_API_TOKEN", "CHAT_ID" 추가 -> 기본값 입력
 * 주의사항! 위 파라미터들이 pipeline parameter에 있으면 안됩니다. 기본값이 바뀝니다.
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
    for(cause in build.getBuildCauses()) {
        if (cause._class.contains("UpstreamCause")) {
            def result = getResult(cause)
            send(result)
        }
    }
}

def getResult(cause) {
    def jenkins = Jenkins.getInstanceOrNull()
    if (jenkins) {
        def upstreamProject = jenkins.getItemByFullName(cause.upstreamProject)
        def upstreamBuild = upstreamProject?.getBuildByNumber(cause.upstreamBuild)
        if (!upstreamProject) {
            return "${cause.upstreamProject} not found."
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = "${cause.upstreamProject} #${cause.upstreamBuild}"
            return "[${title}](${url}) not found."
        } else {
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def marker = getMarker(upstreamBuild.result)
            def title = upstreamBuild.fullDisplayName
            def message = "Build ${upstreamBuild.result.toString().toLowerCase()}."
            def elapsed = "`${upstreamBuild.durationString} elapsed.`"
            def startedBy = "${upstreamBuild.getCauses().collect {"`${it.shortDescription}`"}.join(",\n")}."
            return [
                "[${marker} ${title}](${url})",
                message,
                elapsed,
                startedBy,
            ].findAll {it}.join("\n")
        }
    } else {
        return "Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent."
    }
}

def send(message) {
    sh """
curl 'https://api.telegram.org/bot${params.BOT_API_TOKEN}/sendMessage' \
-s \
-X POST \
-H 'content-type: application/json' \
-d '{\
        "chat_id\":\"${params.CHAT_ID}\",
        \"parse_mode\":\"MarkdownV2\",
        \"text\":\"${escapeSpecialLetter(message)}\"
    }'
"""
}

def escapeSpecialLetter(str) {
    return str.replaceAll(/([.#-])/, '\\\\\\\\$1').replaceAll(/(["])/, '\\\\$1')
}

def getMarker(result) {
    switch (result) {
        case Result.SUCCESS: return "🟢"
        case Result.FAILURE: return "🔴"
        default: return "🟡"
    }
}
