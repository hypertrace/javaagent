package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strconv"

	v1beta1 "k8s.io/api/admission/v1beta1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type patchOperation struct {
	Op    string      `json:"op"`
	Path  string      `json:"path"`
	Value interface{} `json:"value,omitempty"`
}

func getEnv(key, fallback string) string {
	value, exists := os.LookupEnv(key)
	if !exists {
		value = fallback
	}
	return value
}

func createPatch(pod *corev1.Pod) ([]byte, error) {
	patch := []patchOperation{}
	patch = append(patch, addVolume(pod)...)
	patch = append(patch, updateContainers(pod)...)
	patch = append(patch, addInitContainer(pod)...)
	return json.Marshal(patch)
}

func addVolume(pod *corev1.Pod) (patch []patchOperation) {

	volume := corev1.Volume{
		Name: "hypertrace-javaagent",
		VolumeSource: corev1.VolumeSource{
			EmptyDir: &corev1.EmptyDirVolumeSource{},
		},
	}

	path := "/spec/volumes"
	var value interface{}

	if len(pod.Spec.Volumes) != 0 {
		path = path + "/-"
		value = volume
	} else {
		value = []corev1.Volume{volume}
	}

	return append(patch, patchOperation{
		Op:    "add",
		Path:  path,
		Value: value,
	})
}

func getServiceName(pod *corev1.Pod) (serviceName string) {
	if value, found := pod.Labels["app.kubernetes.io/name"]; found {
		return value
	}

	if value, found := pod.Labels["app"]; found {
		return value
	}

	return ""
}

// add volumeMount and JAVA_TOOL_OPTIONS environment variable
func updateContainers(pod *corev1.Pod) (patch []patchOperation) {

	modifiedContainers := []corev1.Container{}
	serviceName := getServiceName(pod)

	for _, container := range pod.Spec.Containers {
		ignore := false
		for _, envVar := range container.Env {
			if envVar.Name == "HYPERTRACE_IGNORE_JAVAAGENT" {
				ignore = true
				break
			}
		}

		if !ignore {
			// update volume mount
			volumeMount := corev1.VolumeMount{
				Name:      "hypertrace-javaagent",
				MountPath: "/mnt/hypertrace",
			}
			container.VolumeMounts = append(container.VolumeMounts, volumeMount)

			//update environment variables
			found := false
			modifiedEnvVars := []corev1.EnvVar{}
			for _, envVar := range container.Env {
				if envVar.Name == "JAVA_TOOL_OPTIONS" {
					envVar.Value = envVar.Value + " -javaagent:/mnt/hypertrace/hypertrace-agent-all.jar"
					found = true
				}
				modifiedEnvVars = append(modifiedEnvVars, envVar)
			}
			if !found {
				modifiedEnvVars = append(modifiedEnvVars, corev1.EnvVar{
					Name:  "JAVA_TOOL_OPTIONS",
					Value: "-javaagent:/mnt/hypertrace/hypertrace-agent-all.jar",
				})
			}

			if serviceName != "" {
				modifiedEnvVars = append(modifiedEnvVars, corev1.EnvVar{
					Name:  "HT_SERVICE_NAME",
					Value: serviceName,
				})
			}

			reportingEndpoint := getEnv("HT_REPORTING_ENDPOINT", "")
			if reportingEndpoint != "" {
				modifiedEnvVars = append(modifiedEnvVars, corev1.EnvVar{
					Name:  "HT_REPORTING_ENDPOINT",
					Value: reportingEndpoint,
				})
			}

			container.Env = modifiedEnvVars
		}

		modifiedContainers = append(modifiedContainers, container)
	}

	return append(patch, patchOperation{
		Op:    "replace",
		Path:  "/spec/containers",
		Value: modifiedContainers,
	})
}

func addInitContainer(pod *corev1.Pod) (patch []patchOperation) {

	initContainer := corev1.Container{
		Image:           getEnv("HYPERTRACE_JAVAAGENT_IMAGE", "hypertrace/javaagent:latest"),
		ImagePullPolicy: "IfNotPresent",
		Name:            "hypertrace-javaagent-init",
		VolumeMounts: []corev1.VolumeMount{
			corev1.VolumeMount{
				Name:      "hypertrace-javaagent",
				MountPath: "/mnt/hypertrace",
			},
		},
	}

	path := "/spec/initContainers"
	var value interface{}

	if len(pod.Spec.InitContainers) != 0 {
		path = path + "/-"
		value = initContainer
	} else {
		value = []corev1.Container{initContainer}
	}

	return append(patch, patchOperation{
		Op:    "add",
		Path:  path,
		Value: value,
	})
}

// Mutate mutates
func mutate(body []byte) ([]byte, error) {
	var err error
	var verbose bool
	verbose, err = strconv.ParseBool(getEnv("DEBUG_ENABLED", "false"))
	if verbose {
		log.Printf("recv: %s\n", string(body))
	}

	// unmarshal request into AdmissionReview struct
	admReview := v1beta1.AdmissionReview{}
	if err := json.Unmarshal(body, &admReview); err != nil {
		return nil, fmt.Errorf("unmarshaling request failed with %s", err)
	}

	var pod *corev1.Pod

	responseBody := []byte{}
	ar := admReview.Request
	resp := v1beta1.AdmissionResponse{}

	if ar != nil {

		// get the Pod object and unmarshal it into its struct, if we cannot, we might as well stop here
		if err := json.Unmarshal(ar.Object.Raw, &pod); err != nil {
			return nil, fmt.Errorf("unable unmarshal pod json object %v", err)
		}
		// set response options
		resp.Allowed = true
		resp.UID = ar.UID
		pT := v1beta1.PatchTypeJSONPatch
		resp.PatchType = &pT

		// generate patch
		resp.Patch, err = createPatch(pod)
		if err != nil {
			return nil, fmt.Errorf("failed to mutate pod %v", err)
		}

		// Success
		resp.Result = &metav1.Status{
			Status: "Success",
		}

		admReview.Response = &resp
		// back into JSON so we can return the finished AdmissionReview w/ Response directly
		// w/o needing to convert things in the http handler
		responseBody, err = json.Marshal(admReview)
		if err != nil {
			return nil, err
		}
	}

	if verbose {
		log.Printf("resp: %s\n", string(responseBody))
	}

	return responseBody, nil
}
