FROM python:3.9-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV IMD_REPO=/opt/IMD
ENV IMD_DEVICE=cpu
ENV IMD_ENABLE_BUSTERNET=false

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        git \
        libglib2.0-0 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN python -m pip install --no-cache-dir --upgrade pip \
    && python -m pip install --no-cache-dir -r requirements.txt \
    && python -m pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu \
    && python -m pip install --no-cache-dir pytorch-lightning

RUN git clone --depth 1 https://github.com/gagan3012/IMD.git /opt/IMD

COPY app.py .

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
