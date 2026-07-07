{{- define "autolog.name" -}}
{{- default .Chart.Name .Values.appName | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "autolog.labels" -}}
app.kubernetes.io/name: {{ include "autolog.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "autolog.selectorLabels" -}}
app.kubernetes.io/name: {{ include "autolog.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "autolog.backendName" -}}
{{ include "autolog.name" . }}-backend
{{- end -}}

{{- define "autolog.frontendName" -}}
{{ include "autolog.name" . }}-frontend
{{- end -}}

{{- define "autolog.backendSecretName" -}}
{{- if .Values.backend.secrets.create -}}
{{ include "autolog.backendName" . }}-secrets
{{- else -}}
{{ required "backend.secrets.existingSecret is required when backend.secrets.create=false" .Values.backend.secrets.existingSecret }}
{{- end -}}
{{- end -}}
