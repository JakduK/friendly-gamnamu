import jenkins.model.Jenkins
import hudson.model.Result

/**
 * 빌드 결과 텔레그램 알림.
 *
 * 파라미터 설정.
 * 주의사항! 아래 파라미터들이 pipeline parameters에 있으면 안됩니다. 기본값이 바뀝니다.
 * - Job Configure -> This project is parameterized 체크. 파라미터 추가후 기본값 입력.
 * - String Parameter "BOT_API_TOKEN" : 텔레그램 bot api token.
 * - String Parameter "CHAT_ID" : 텔레그램 채팅방 아이디.
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
            return [
                title:cause.upstreamProject,
                message:"Not found.",
            ]
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = "${cause.upstreamProject} #${cause.upstreamBuild}"

            return [
                title:title,
                url:url,
                message:"Not found.",
            ]
        } else {
            def parameterList = upstreamBuild.getAction(ParametersAction)
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def marker = getResultMarker(upstreamBuild.result)
            def title = upstreamBuild.fullDisplayName
            def message = "Build ${upstreamBuild.result.toString().toLowerCase()}."
            def elapsed = "${upstreamBuild.durationString} elapsed."
            def startedBys = upstreamBuild.getCauses().collect{it.shortDescription}
            def parameters = (parameterList ? parameterList.getParameters() : [])
            def changes = upstreamBuild.getChangeSets().collect{
                it.getItems().collect{"${it.commitId.substring(0, 7)} ${it.msg} (by ${it.authorEmail})"}
            }.flatten()

            return [
                title:"${marker} ${title}",
                url:url,
                message: [
                    message,
                    elapsed,
                ].join("\n"),
                startedBys:startedBys,
                parameters:parameters,
                changes:changes,
            ]
        }
    } else {
        return [
            title:"Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent.",
            message:"Unavailable.",
        ]
    }
}

def send(result) {
    sh """
curl 'https://api.telegram.org/bot${params.BOT_API_TOKEN}/sendMessage' \
-s \
-X POST \
-H 'content-type: application/json' \
-d '
{
    "chat_id":"${params.CHAT_ID}",
    "parse_mode":"MarkdownV2",
    "text":"${[
        "${result.url ? "[${encodeMarkdownInShell(result.title)}](${encodeMarkdownInShell(result.url)})" : result.title}",
        "${encodeMarkdownInShell(result.message)}",
        "${!result.startedBys ? "" : "`${encodeMarkdownInShell(result.startedBys.collect{"‣ ${it}"}.join("\n"))}"}`",
        " ",
        "*Build Parameters*",
        "${!result.parameters ? "" : "`${encodeMarkdownInShell(result.parameters.collect{"‣ ${it.name} : ${it.value}"}.join("\n"))}"}`",
        " ",
        "*Changes*",
        "`${encodeMarkdownInShell(result.changes.collect{"‣ ${it}"}.join("\n"))}`",
    ].findAll{it}.join("\n")}"
}'
"""
}

def encodeMarkdownInShell(str) {
    return !str ? "" :
    str.trim()
    .replaceAll(/(['])/, '\\\\$1')
    .replaceAll(/(["])/, '\\\\\\\\$1')
    .replaceAll(/([`#.\-_*|+(){}<>\[\]])/, '\\\\\\\\\\\\\$1')
}

def getResultMarker(result) {
    switch (result) {
        case Result.SUCCESS: return "🟢"
        case Result.FAILURE: return "🔴"
        default: return "🟡"
    }
}
