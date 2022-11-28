FROM python:3.9-slim-bullseye
WORKDIR /app
ENV APP_PATH=services/worker
COPY . .
RUN cp -r common/ $APP_PATH/common
RUN pip install -r $APP_PATH/requirements.txt


CMD ["python3", "services/worker/app.py"]