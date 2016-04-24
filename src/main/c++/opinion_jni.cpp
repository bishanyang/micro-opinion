#include "edu_cmu_ml_rtw_micro_opinion_OpinionExtractor.h"

#include <jni.h>
#include <stdlib.h>
#include <string.h>

extern "C" {
void initialize(const char* res, const char* wordvec,
		const char* adt_model, const char* dse_model, const char* polarity_model);

char* opinion_parse(const char* input_data);
}

JNIEXPORT jboolean JNICALL Java_edu_cmu_ml_rtw_micro_opinion_OpinionExtractor_initialize(
		JNIEnv* env, jobject obj, jstring res, jstring wordvec,
		jstring adt_model, jstring dse_model, jstring polarity_model)
{
	const char* res_str = env->GetStringUTFChars(res, 0);
	if (res_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'res_str'.\n");
		return false;
	}

	const char* wordvec_str = env->GetStringUTFChars(wordvec, 0);
	if (wordvec_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'wordvec_str'.\n");
		return false;
	}

	const char* adt_model_str = env->GetStringUTFChars(adt_model, 0);
	if (adt_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'adt_model_str'.\n");
		return false;
	}

	const char* dse_model_str = env->GetStringUTFChars(dse_model, 0);
	if (dse_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'dse_model_str'.\n");
		return false;
	}

	const char* polarity_model_str = env->GetStringUTFChars(polarity_model, 0);
	if (polarity_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'polarity_model_str'.\n");
		return false;
	}

	initialize(res_str, wordvec_str, adt_model_str, dse_model_str, polarity_model_str);

	env->ReleaseStringUTFChars(res, res_str);
	env->ReleaseStringUTFChars(wordvec, wordvec_str);
	env->ReleaseStringUTFChars(adt_model, adt_model_str);
	env->ReleaseStringUTFChars(dse_model, dse_model_str);
	env->ReleaseStringUTFChars(polarity_model, polarity_model_str);

	return true;
}

JNIEXPORT jstring JNICALL Java_edu_cmu_ml_rtw_micro_opinion_OpinionExtractor_annotate(
		JNIEnv* env, jobject obj, jstring input_data)
{
	const char* input_str = env->GetStringUTFChars(input_data, 0);
	if (input_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'sentence'.\n");
		return NULL;
	}

	char* result = opinion_parse(input_str);
	if (result == NULL) {
		env->ReleaseStringUTFChars(input_data, input_str);
		return NULL;
	}
	jstring result_str = env->NewStringUTF(result);

	env->ReleaseStringUTFChars(input_data, input_str);
	free(result);

	return result_str;
}
