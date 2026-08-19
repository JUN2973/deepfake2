# IMD adapter service

This service wraps `gagan3012/IMD` as a JSON API that the Spring Boot app can call.

## Setup

```powershell
git clone https://github.com/gagan3012/IMD C:\SpringBootWorks\IMD
cd C:\SpringBootWorks\deepfake2\imd-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
pip install -r C:\SpringBootWorks\IMD\requirements.txt
$env:IMD_REPO="C:\SpringBootWorks\IMD"
python -m uvicorn app:app --host 127.0.0.1 --port 18080
```

Then run Spring Boot with:

```properties
deepfake.client.mode=imd
imd.base-url=http://127.0.0.1:18080
```

The upstream IMD repository uses ManTraNet and BusterNet. Its dependency stack includes old TensorFlow/Keras components, so Python version and package compatibility matter. If BusterNet cannot run in your local environment, this adapter still returns the ManTraNet heatmap when available.
