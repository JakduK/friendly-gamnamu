import jenkins.model.Jenkins
import hudson.model.Result
import hudson.model.ParametersAction

/**
 * 빌드 결과 슬랙 알림.
 *
 * 파라미터 설정.
 * 주의사항! 아래 파라미터들이 pipeline parameters에 있으면 안됩니다. 기본값이 바뀝니다.
 * - Job Configure -> This project is parameterized 체크. 파라미터 추가후 기본값 입력.
 * - Multi-line String Parameter "COMMON_WEB_HOOK_URLS" : 빌드 결과 전송 웹훅 URL.
 * - Multi-line String Parameter "SUCCESS_WEB_HOOK_URLS" : 성공시 전송 웹훅 URL.
 * - Multi-line String Parameter "FAILURE_WEB_HOOK_URLS" : 실패시 전송 웹훅 URL.
 *
 * 알림 받을 잡 관리.
 * Build Triggers -> Build after other projects are built 체크 -> Trigger even if the build fails 체크.
 * Projects to watch 알림 받을 잡 등록.
 *
 * In-process Script Approval 필수 허용 항목들.
 * - method hudson.model.Actionable getAction java.lang.Class
 * - method hudson.model.Cause getShortDescription
 * - method hudson.model.Item getUrl
 * - method hudson.model.Job getBuildByNumber int
 * - method hudson.model.ParameterValue getName
 * - method hudson.model.ParameterValue getValue
 * - method hudson.model.ParametersAction getParameters
 * - method hudson.model.Run getCauses
 * - method hudson.model.Run getDurationString
 * - method hudson.model.Run getResult
 * - method hudson.model.Run getUrl
 * - method jenkins.scm.RunWithSCM getChangeSets
 * - method hudson.plugins.git.GitChangeSet getAuthorEmail
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
                    sendUpstreamBuildResult(currentBuild)
                }
            }
        }
    }
}

def sendUpstreamBuildResult(build) {
    for (cause in build.getBuildCauses()) {
        if (cause._class.contains("UpstreamCause")) {
            def result = getResult(cause)
            send(result, splitLines(params.COMMON_WEB_HOOK_URLS))
            if (result.status == Result.SUCCESS) {
                send(result, splitLines(params.SUCCESS_WEB_HOOK_URLS))
            } else if (result.status == Result.FAILURE) {
                send(result, splitLines(params.FAILURE_WEB_HOOK_URLS))
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
                title:"[${Result.FAILURE}] ${cause.upstreamProject}",
                message:"Project not found.",
            ]
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = "[${Result.FAILURE}] ${cause.upstreamProject} #${cause.upstreamBuild}"

            return [
                status:Result.FAILURE,
                title:title,
                url:url,
                message:"Build not found.",
            ]
        } else {
            def parameterList = upstreamBuild.getAction(ParametersAction)
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def title = "[${upstreamBuild.result}] ${upstreamBuild.fullDisplayName}"
            def elapsed = "_${upstreamBuild.durationString} elapsed._"
            def startedBy = "${upstreamBuild.getCauses().collect{"‣ _${it.shortDescription}_"}.join(",\n")}."
            def parameters = (parameterList ? parameterList.getParameters() : [])
            def changes = upstreamBuild.getChangeSets().collect{
                it.getItems().collect{"${it.commitId.substring(0, 7)} ${it.msg} (by ${it.authorEmail})"}
            }.flatten()

            return [
                status:upstreamBuild.result,
                title:title,
                url:url,
                message: [
                    elapsed,
                    startedBy,
                ].join("\n"),
                parameters: parameters,
                changes: changes,
            ]
        }
    } else {
        return [
            status:Result.FAILURE,
            title:"Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent.",
            message:"Unavailable.",
        ]
    }
}

def send(result, webHooks) {
    def color = getStatusColor(result.status)
    for (webHook in webHooks) {
        sh """
curl '${webHook}' \
-s \
-X POST \
-H 'content-type: application/json' \
-d '
{
    "blocks":[
        {
            "type":"section",
            "text":{
                "type":"mrkdwn",
                "text":"*${encodeJsonStringInShell(result.url ? "<${result.url}|${result.title}>" : result.title)}*"
            }
        }
   ],
    "attachments":[
        {
            "color":"${color}",
            "blocks":[
                ${[
                """
                {
                    "type":"section",
                    "text":{
                        "type":"mrkdwn",
                        "text":"${encodeJsonStringInShell(result.message)}"
                    }
                }
                """,
                """
                {
                    "type":"section",
                    "text":{
                        "type":"mrkdwn",
                        "text":"*Build Parameters*"
                    }
                }
                """,
                !result.parameters ? "" :
                """
                {
                    "type":"section",
                    "text":{
                        "type":"mrkdwn",
                        "text":"```${encodeJsonStringInShell(formatParameters(result.parameters))}```"
                    }
                }
                """,
                """
                {
                    "type":"section",
                    "text":{
                        "type":"mrkdwn",
                        "text":"*Changes*"
                    }
                }
                """,
                !result.changes ? "" :
                """
                {
                    "type":"section",
                    "text":{
                        "type":"mrkdwn",
                        "text":"${encodeJsonStringInShell(formatChanges(result.changes))}"
                    }
                }
                """
                ].findAll{it}.join(",")}
            ]
        }
    ]
}
'
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

def encodeJsonStringInShell(str) {
    return !str ? "" :
        str.trim()
        .replaceAll(/(["\\])/, '\\\\$1')
        .replaceAll(/(')/, "'\\\\''")
}

def splitLines(str) {
    return str ? str.trim().split("\n").findAll{it.trim()} : []
}

def formatChanges(lines) {
    def result = []
    def sum = 20 + lines.size() * 4
    for (line in lines) {
        sum += line.length()
        if (sum > 3000) {
            break
        } else {
            result.add(line)
        }
    }

    def formatted = result.collect{"‣ ${it}"}.join("\n\n")

    return result.size() == lines.size()
        ? formatted
        : "${formatted}\n\n${lines.size() - result.size()} more changes."
}

def formatParameters(parameters) {
    return parameters.collect{"*** ${it.name} ***\n${it.value}"}.join("\n\n")
}