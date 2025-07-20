FROM 123marvin123/typst:0.13.1 AS typst-builder

FROM debian:bullseye AS ssl-builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends libssl1.1 && \
    rm -rf /var/lib/apt/lists/*

FROM sbtscala/scala-sbt:graalvm-community-22.0.1_1.10.7_2.13.15 AS app

ARG UID
ARG GID
ARG USERNAME=inline-typst-bot

COPY --from=typst-builder /usr/bin/typst /usr/local/bin/typst
COPY --from=ssl-builder /usr/lib/x86_64-linux-gnu/libssl.so.1.1 /usr/lib/x86_64-linux-gnu/libssl.so.1.1

RUN groupadd --gid ${GID} ${USERNAME} \
 && useradd --uid ${UID} --gid ${GID} --shell /bin/bash --create-home ${USERNAME}

ENV HOME=/home/${USERNAME}
WORKDIR ${HOME}

RUN mkdir -p ${HOME}/.sbt ${HOME}/.ivy2 ${HOME}/cache && \
    chown -R ${USERNAME}:${USERNAME} ${HOME}

USER ${USERNAME}

COPY --chown=${UID}:${GID} . .

RUN sbt compile

CMD ["sbt", "run"]
