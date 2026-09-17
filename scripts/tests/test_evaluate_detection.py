import argparse
import csv
from pathlib import Path
import sys
import tempfile
import unittest


SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

import evaluate_detection as evaluation


class DetectionEvaluationTest(unittest.TestCase):

    def setUp(self):
        self.rows = [
            {"label": 1, "score": 0.9},
            {"label": 1, "score": 0.4},
            {"label": 0, "score": 0.6},
            {"label": 0, "score": 0.1},
        ]

    def test_calculates_binary_metrics_and_auc(self):
        metrics = evaluation.calculate_metrics(self.rows, 0.5)

        self.assertEqual({"tp": 1, "fp": 1, "tn": 1, "fn": 1}, metrics["confusion_matrix"])
        self.assertAlmostEqual(0.5, metrics["accuracy"])
        self.assertAlmostEqual(0.5, metrics["precision"])
        self.assertAlmostEqual(0.5, metrics["recall"])
        self.assertAlmostEqual(0.5, metrics["specificity"])
        self.assertAlmostEqual(0.5, metrics["f1"])
        self.assertAlmostEqual(0.75, metrics["roc_auc"])
        self.assertAlmostEqual(5.0 / 6.0, metrics["average_precision"])

    def test_selects_threshold_using_f1(self):
        threshold, metrics = evaluation.select_threshold(self.rows)

        self.assertAlmostEqual(0.4, threshold)
        self.assertAlmostEqual(0.8, metrics["f1"])

    def test_auc_handles_tied_scores(self):
        labels = [1, 0]
        scores = [0.5, 0.5]

        self.assertAlmostEqual(0.5, evaluation.roc_auc(labels, scores))
        self.assertAlmostEqual(0.5, evaluation.average_precision(labels, scores))

    def test_load_rows_separates_detector_errors(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "predictions.csv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(
                    handle,
                    fieldnames=["sample_id", "ground_truth", "split", "score", "error"],
                )
                writer.writeheader()
                writer.writerow({
                    "sample_id": "ok-1", "ground_truth": "FAKE", "split": "test",
                    "score": "0.8", "error": "",
                })
                writer.writerow({
                    "sample_id": "bad-1", "ground_truth": "REAL", "split": "test",
                    "score": "", "error": "timeout",
                })

            valid, invalid = evaluation.load_rows(path)

        self.assertEqual(1, len(valid))
        self.assertEqual(1, len(invalid))
        self.assertIn("detector error", invalid[0]["reason"])

    def test_report_uses_validation_threshold_and_reports_test_coverage(self):
        fixture = Path(__file__).parent / "fixtures" / "predictions.csv"
        valid, invalid = evaluation.load_rows(fixture)
        args = argparse.Namespace(
            threshold=0.5,
            selection_split="validation",
            evaluation_split="test",
            bootstrap_samples=20,
            seed=2973,
            min_subgroup_size=2,
        )

        report = evaluation.build_report(valid, invalid, args)

        self.assertAlmostEqual(0.4, report["protocol"]["threshold"])
        self.assertEqual(4, report["evaluation"]["metrics"]["support"])
        self.assertAlmostEqual(0.8, report["evaluation"]["coverage"])
        self.assertAlmostEqual(1.0, report["evaluation"]["metrics"]["f1"])

    def test_report_rejects_duplicate_file_hashes(self):
        rows = [
            {
                "sample_id": "one", "label": 1, "score": 0.9, "split": "test",
                "source": "source", "manipulation_type": "type", "provider": "provider",
                "model_version": "v1", "file_sha256": "abc", "latency_ms": 1.0,
            },
            {
                "sample_id": "two", "label": 0, "score": 0.1, "split": "test",
                "source": "source", "manipulation_type": "none", "provider": "provider",
                "model_version": "v1", "file_sha256": "abc", "latency_ms": 1.0,
            },
        ]
        args = argparse.Namespace(
            threshold=0.5, selection_split="", evaluation_split="test",
            bootstrap_samples=0, seed=2973, min_subgroup_size=1,
        )

        with self.assertRaisesRegex(ValueError, "duplicate file_sha256"):
            evaluation.build_report(rows, [], args)


if __name__ == "__main__":
    unittest.main()
