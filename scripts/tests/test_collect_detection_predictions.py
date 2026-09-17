from pathlib import Path
import sys
import tempfile
import unittest


SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

import collect_detection_predictions as collector


class DetectionPredictionCollectorTest(unittest.TestCase):

    def test_sha256_is_stable(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "sample.jpg"
            path.write_bytes(b"deepfake-benchmark")

            digest = collector.file_sha256(path)

        self.assertEqual(
            "22efa02afdbc9564161b4c6700a2f7a5cb2fe7cb379a9db78e3ca9d42d0c208d",
            digest,
        )

    def test_multipart_body_sanitizes_filename(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / 'sample.jpg'
            path.write_bytes(b"image-bytes")

            body, content_type = collector.multipart_body("file", path)

        self.assertIn("multipart/form-data; boundary=", content_type)
        self.assertIn(b'name="file"; filename="sample.jpg"', body)
        self.assertIn(b"image-bytes", body)


if __name__ == "__main__":
    unittest.main()
