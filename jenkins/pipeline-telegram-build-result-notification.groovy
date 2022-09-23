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
            return escapeSpecialLetter("${cause.upstreamProject} not found.")
        } else if (!upstreamBuild) {
            def url = "${jenkins.rootUrl}${upstreamProject.url}"
            def title = "${cause.upstreamProject} #${cause.upstreamBuild}"

            return "[${escapeSpecialLetter(title)}](${url}) ${escapeSpecialLetter("not found.")}"
        } else {
            def parameterList = upstreamBuild.getAction(ParametersAction)
            def url = "${jenkins.rootUrl}${upstreamBuild.url}"
            def marker = getMarker(upstreamBuild.result)
            def title = upstreamBuild.fullDisplayName
            def message = "Build ${upstreamBuild.result.toString().toLowerCase()}."
            def elapsed = "${upstreamBuild.durationString} elapsed."
            def startedBy = "${upstreamBuild.getCauses().collect {"‣ `${it.shortDescription}`"}.join(",\n")}."
            def parameters = (parameterList ? parameterList.getParameters() : []).collect{param ->
                return "`‣ ${param.name} : ${param.value} `"
            }
            def changes = upstreamBuild.getChangeSets().collect {change ->
                return change.getItems().collect {item ->
                    return "`‣ ${item.commitId.substring(0, 7)} ${item.msg} (by ${item.authorEmail}) `"
                }
            }.flatten()

            return [
                title:"${marker} ${title}",
                url:url,
                message: [
                    message,
                    elapsed,
                    startedBy,
                ].join("\n"),
                parameters: parameters,
                changes: changes,
            ]
        }
    } else {
        return "Jenkins service has not been started, or was already shut down, or we are running on an unrelated JVM, typically an agent."
    }
}

def send(result) {
    sh """
curl 'https://api.telegram.org/bot${params.BOT_API_TOKEN}/sendMessage' \
-s \
-X POST \
-H 'content-type: application/json' \
-d "{
        \\"chat_id\\":\\"${params.CHAT_ID}\\",
        \\"parse_mode\\":\\"MarkdownV2\\",
        \\"text\\":\\"${
        """
[${escapeSpecialLetter(result.title)}](${escapeSpecialLetter(result.url)})
${escapeSpecialLetter(result.message)}

*Build Parameters*
${!result.parameters ? "" : "${escapeSpecialLetter(result.parameters.join("\n"))}\n"}
*Changes*
${escapeSpecialLetter(result.changes.join("\n"))}
        """
        }\\"
    }"
"""
}

def escapeSpecialLetter(str) {
    return !str ? "" :
    str.trim()
    .replaceAll(/(["!\\])/, '\\\\\\\\\\\\$1')
    .replaceAll(/([#.\-_*|+(){}<>])/, '\\\\\\\\\\\\\\\\$1')
    .replaceAll(/([`])/, '\\\\$1')
}

def getMarker(result) {
    switch (result) {
        case Result.SUCCESS: return "🟢"
        case Result.FAILURE: return "🔴"
        default: return "🟡"
    }
}
