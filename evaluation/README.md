# DeepScan detection benchmark

This directory defines a reproducible protocol for measuring detection quality.
It deliberately does not contain a claimed accuracy value: a real result must be
generated from independently labelled images, never from model predictions used
as their own ground truth.

## 1. Prepare a labelled manifest

Copy `dataset-template.csv` and add one row per image:

```csv
sample_id,file_path,ground_truth,split,source,manipulation_type
real-001,data/real/001.jpg,REAL,validation,internal-camera,none
fake-001,data/fake/001.jpg,FAKE,validation,generator-a,face-swap
real-101,data/real/101.jpg,REAL,test,internal-camera,none
fake-101,data/fake/101.jpg,FAKE,test,generator-b,reenactment
```

Rules:

- `sample_id` must be unique and must not reveal personal information.
- `ground_truth` accepts `REAL` or `FAKE` only.
- Use separate `validation` and `test` splits.
- Keep the same person, source video, or near-duplicate images in one split only.
- Do not choose or alter the test set after seeing its results.
- Record the dataset/source and manipulation type so subgroup failures remain visible.
- Keep licensed image files under `evaluation/data/`; that directory is gitignored.

Aim for balanced classes and enough samples per source. A small classroom pilot can
start with 100 real and 100 fake images, but its confidence intervals will be wide
and it must not be presented as production-level evidence.

## 2. Collect predictions

Start DeepScan with the real detector configuration, then run:

```powershell
.venv\Scripts\python.exe scripts\collect_detection_predictions.py `
  --manifest evaluation\dataset.csv `
  --output evaluation\output\predictions.csv `
  --base-url http://127.0.0.1:11000 `
  --model-version reality-defender-YYYY-MM-DD
```

The collector uploads each image through DeepScan's existing verification API and
records the manipulation-risk score, verdict, provider, latency, and any error.
It keeps the HTTP session so anonymous result details can be read, and it resumes
without submitting sample IDs already present in the output file.
Each file's SHA-256 is recorded, and evaluation stops if an exact duplicate would
inflate the result. Evaluation also rejects mixed `model_version` values so one
headline number always refers to one frozen detector configuration.

External APIs may cost money. Run a small pilot first, verify the selected provider,
and never use dummy mode for a published benchmark.

## 3. Generate the report

First choose a threshold using only the validation split and evaluate the locked
threshold on the test split:

```powershell
.venv\Scripts\python.exe scripts\evaluate_detection.py `
  --input evaluation\output\predictions.csv `
  --selection-split validation `
  --evaluation-split test `
  --output-dir evaluation\output\report
```

To evaluate a pre-declared fixed threshold instead:

```powershell
.venv\Scripts\python.exe scripts\evaluate_detection.py `
  --input evaluation\output\predictions.csv `
  --threshold 0.5 `
  --evaluation-split test `
  --output-dir evaluation\output\report-fixed
```

Outputs:

- `metrics.json`: machine-readable protocol, counts, metrics, and confidence intervals.
- `report.md`: presentation-ready summary and subgroup tables.

The primary report includes coverage, confusion matrix, accuracy, precision, recall,
specificity, F1, balanced accuracy, ROC-AUC, average precision, Brier score, and
latency percentiles. Bootstrap 95% confidence intervals are included so a metric is
not presented as more certain than the sample size supports.

## 4. Interpretation

- Recall answers: "Of all fake images, how many did we catch?"
- Precision answers: "Of images flagged fake, how many were actually fake?"
- Specificity answers: "Of real images, how many did we avoid falsely flagging?"
- F1 balances precision and recall.
- ROC-AUC measures ranking quality across thresholds.
- Average precision is especially useful when fake images are rare.
- Brier score measures probability error; lower is better.
- Coverage reports how often the detector returned an evaluable score.

Always publish the dataset composition, split policy, threshold-selection rule,
model/provider version, error count, and confidence interval with the headline F1.
