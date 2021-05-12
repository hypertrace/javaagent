package main

import "testing"

func TestToEnvFormatSuccess(t *testing.T) {
	if want, have := "MY_VAR", toEnvFormat("myVar"); want != have {
		t.Errorf("Unexpected env format, want %q, have %q", want, have)
	}
}

func TestToFieldDescriptionSuccess(t *testing.T) {
	if want, have := "Does something else", toFieldDescription(
		"something",
		"something does something else",
	); want != have {
		t.Errorf("Unexpected env format, want %q, have %q", want, have)
	}
}
