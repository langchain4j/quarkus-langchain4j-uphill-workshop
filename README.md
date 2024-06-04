# Quarkus-LangChain4j Workshop

### What you need for this workshop
- JDK 17.0 or later
- A key for OpenAI API
- Optional: a key for Cohere API (you can get it [here](https://dashboard.cohere.com/welcome/login?redirect_uri=%2Fapi-keys)) if you want to add reranking at the end


Good to know:

1. To run the application in dev mode:
```shell script
./mvnw quarkus:dev
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

or if you prefer to use the maven wrapper:
```shell
./mvnw quarkus:dev
```

> **_NOTE:_** If you run into an error about the mvnw maven wrapper, you can give execution permission for the file by navigating to the project folder and executing `chmod +x mvnw`.

or if you installed the [Quarkus CLI](https://quarkus.io/guides/cli-tooling), you can also use:
```shell
quarkus dev
```

This will bring up the page at `localhost:8080`. Open it and click the red
robot icon in the bottom right corner to start chatting with the chatbot.
The chatbot is calling GPT-4o (OpenAI) via the backend. You can test it out
and observe that it has memory.

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

## STEP 2
Play around with the model parameters in
`src/main/resources/application.properties`. If you don’t have
autocompletion, you can search through them in the Quarkus DevUI at
`localhost:8080/q/dev` under `Configuration` (use the filter
to find properties containing `openai.chat-model`).

> **_IMPORTANT:_** After changing a configuration property, you need to
force a restart the application to apply the changes. Simply submitting a
new chat message in the UI does not trigger it (it only sends a websocket
message rather than an HTTP request), so you have to refresh the page in
your browser.

The precise meaning of most model parameters is described on the website of
OpenAI: https://platform.openai.com/docs/api-reference/chat/create

Examples to try:

- `quarkus.langchain4j.openai.chat-model.temperature` controls the
  randomness of the model's responses. Lowering the temperature will make the
  model more conservative, while increasing it will make it more creative. Try
  asking "Describe a sunset over the mountains" while setting the temperature
  to 0.1 and then to, say, 1.5, and observe the different style of the
  response. With a too high temperature over 1.5, the model often starts
  producing garbage, or fails to produce a valid response at all.

- `quarkus.langchain4j.openai.chat-model.max-tokens` limits the length of the
  response. Try setting it to 50 and see how the model cuts off the response
  after 50 tokens.

- `quarkus.langchain4j.openai.chat-model.frequency-penalty` defines how much
  the model should avoid repeating itself. Try setting the penalty to 2 (which
  is the maximum for OpenAI models) and see how the model tries to avoid
  repeating words in a single response. For example, ask it to "Repeat the
  word hedgehog 50 times". While with frequency penalty around 0, the model
  gladly repeats the word 50 times, but with 2, it will most likely start
  producing garbage after repeating the word a few times.

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
