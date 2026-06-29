from fastapi import FastAPI

app = FastAPI(title="Crescendo AIML Service")

@app.get("/")
def health_check():
    return {"status": "ok", "service": "crescendo-aiml"}