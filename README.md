# Quarkus-LangChain4j Workshop

### What you need for this workshop
- JDK 17.0 or later
- A key for OpenAI API
- Optional: a key for Cohere API (you can get it here) if you want to add reranking at the end


Good to know:

1. To run the application in dev mode:
```shell script
./mvnw compile quarkus:dev
```

2. Open the app at http://localhost:8080/.
> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

3. For debugging a running Quarkus application, put your breakpoints and select Run > Attach to Process, then select the Quarkus process (in IntelliJ)

## How this workshop works
During this workshop we will create an LLM-powered customer support agent chatbot for a car rental company in 7 steps. We start from the base functionality (step 1) and add features in the subsequent steps. The result after each step is located in a separate directory (`stepXX`). The final solution is in the `step07` directory.

We recommend to start by checking out the `main` branch and then opening the
project from `step01` in your IDE and using that directory throughout the
workshop. The other option is to make a copy of it. If you later need to
reset to a particular step, either overwrite your working directory with the
directory for the step you want to reset to, or, in your IDE, open the
project from the step directory you want to reset to.

If you make any changes to the `stepXX` directories, you can always reset them back by executing:
```shell
git restore stepXX
```

> **_NOTE:_** This will make you lose all your local changes!

Before actually starting the workshop, make sure you have set the OpenAI API
key as an environment variable: 

```shell
export OPENAI_API_KEY=<your-key>
```

and if you're going to use Cohere for reranking (step 7), you'll also need the Cohere API key:
```shell
export COHERE_API_KEY=<your-key>
```

Let's get started!

## STEP 0 

Download the workshop either by cloning the repository on your machine or downloading the zip file:

```
git clone https://github.com/langchain4j/quarkus-langchain4j-uphill-workshop.git
```

Or: 

```
curl -L -o workshop.zip https://github.com/langchain4j/quarkus-langchain4j-uphill-workshop/archive/refs/heads/main.zip
```

## STEP 1
To get started, make sure you use the `step01` directory, or create a copy of it.

This is a functioning skeleton for a web app with a chatbot. You can run it as follows
```shell
mvn quarkus:dev
```
or if you installed Quarkus CLI
```shell
quarkus dev
```

This will bring up the page at `localhost:8080`
The chatbot is calling GPT-4o (OpenAI) via the backend. You can test it out and observe that it has memory.

Example:
```
User: My name is Klaus.
AI: Hi Klaus, nice to meet you.
User: What is my name?
AI: Your name is Klaus.
```

This is how memory is built up for LLMs

<img src='images/chatmemory.png' alt='Chat Memory Concept' width = '450'>

In the console, you can observe the calls that are made to OpenAI behind the scenes, notice the roles 'user' (`UserMessage`) and 'assistant' (`AiMessage`).

If you run into an error about the mvnw maven wrapper, you can give execution permission for the file by navigating to the project folder and executing
```shell
chmod +x mvnw
```

## STEP 2
Play around with the model parameters in 'resources/application.properties'
If you don’t have autocompletion, you can search through them in the Quarkus DevUI at `localhost:8080/q/dev` under `Configuration`.

The precise meaning of most model parameters is described on the website of OpenAI: https://platform.openai.com/docs/api-reference/chat/create

## STEP 3
Instead of passing the response as one block of text when it is ready, enable streaming mode. This will allow us to display the reply token per token, while they come in.
Sorry for what it looks like in the current frontend - do revert it back before moving to step 4 ;)

The problem is that the model does not know it's role.
Example:
```
User: Can I cancel my booking?
AI: No, I don't know what you are talking about ...
```

## STEP 4
Add a `SystemMessage` so the model knows it is a car rental customer assistant.
Observe that the agent is now happy to help with bookings, but does make rules up when it comes to cancellation period.

Example:
```
User: Can I cancel my booking?
AI: Yes, I can help you with that ...
User: What is the cancellation period?
AI: [some nonsense]
```

## STEP 5
Add a RAG system that allows the chatbot to use relevant parts of our Terms of Use (you can find them [here](https://github.com/LizeRaes/quarkus-langchain4j-uphill-workshop/blob/main/src/main/resources/data/miles-of-smiles-terms-of-use.txt)) for answering the customers.

1. Ingestion phase: the documents (files, websites, ...) are loaded, splitted, turned into meaning vectors (embeddings) and stored in an embedding store

<img src='images/ingestion.png' alt='Ingestion' width = '400'>

2. Retrieval phase: with every user prompt, the relevant fragments of our documents are collected by comparing the meaning vector of the prompt with the vectors in the embedding store. The relevant segments are then passed along to the model together with the original question.

<img src='images/retrieval.png' alt='Retrieval' width = '400'>

More info on easy RAG in Quarkus can be found [here](https://docs.quarkiverse.io/quarkus-langchain4j/dev/easy-rag.html).

This is already much better, but it still cannot really perform any booking or cancellation.

Example:
```
User: What is the cancellation period?
AI: ... 11 days ... 4 days ...
User: Cancel my booking
AI: [some nonsense]
```

## STEP 6
Let’s give the model two (dummy) tools to do so:
```java
public Booking getBookingDetails(String customerFirstName, String customerSurname, String bookingNumber) throws BookingNotFoundException
public void cancelBooking(String customerFirstName, String customerSurname, String bookingNumber) throws BookingNotFoundException, BookingCannotBeCancelledException
```
And observe how the chatbot behaves now.
You can ensure that the methods are called by either logging to console, or by putting a breakpoint.

Example:
```
User: Cancel my booking
AI: Please provide your booking number, name and surname...
```

## STEP 7
Let’s make RAG better: add a RetrievalAugmentor with a QueryCompressor and a Reranker (using your Cohere key)
More details on advanced RAG can be found [here](https://github.com/langchain4j/langchain4j/pull/538).
