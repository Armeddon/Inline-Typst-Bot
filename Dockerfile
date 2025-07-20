FROM 123marvin123/typst:0.13.1 AS typst

FROM sbtscala/scala-sbt:graalvm-community-22.0.1_1.10.7_2.13.15 AS app

ARG UID
ARG GID
ARG USERNAME=inline-typst-bot

COPY --from=typst /usr/bin/typst /usr/local/bin/typst

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
