import jenkins.model.Jenkins
import hudson.model.Result

/**
 * 텔레그램 bot api token, chat id 설정 방법.
 * Job Configure -> This project is parameterized 체크 -> String Parameter "BOT_API_TOKEN", "CHAT_ID" 추가 -> 기본값 입력
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
            send(result)
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
            return escapeSpecialLetter("${cause.upstreamProject} not found.")
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = escapeSpecialLetter("${cause.upstreamProject} #${cause.upstreamBuild}")
            return "[${title}](${url})" + escapeSpecialLetter(" not found.")
        } else {
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def title = escapeSpecialLetter(upstreamBuild.fullDisplayName)
            def marker = getMarker(upstreamBuild.result)
            def message = escapeSpecialLetter("Build ${upstreamBuild.result.toString().toLowerCase()}.")
            def elapsed = escapeSpecialLetter("`${upstreamBuild.durationString} elapsed.`")
            def startedBy = escapeSpecialLetter("${upstreamBuild.getCauses().collect {"`${it.shortDescription}`"}.join(",\n")}.")
            return [
                "[${marker} ${title}](${url})",
                message,
                elapsed,
                startedBy,
            ].findAll {it}.join("\n")
        }
    } else {
        return escapeSpecialLetter("Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent.")
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
        \"text\":\"${message}\"
    }'
"""
}

@NonCPS
def escapeSpecialLetter(str) {
    return str.replaceAll(/([#-.])/, '\\\\\\\\$1').replaceAll(/(["])/, '\\\\$1')
}

@NonCPS
def getMarker(result) {
    switch (result) {
        case Result.SUCCESS: return "🟢"
        case Result.FAILURE: return "🔴"
        default: return "🟡"
    }
}
