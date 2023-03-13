FROM ubuntu:22.04

RUN apt-get update \
    && apt-get install -y git wget python3.9 build-essential jq \
    && apt-get clean && apt-get autoclean && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/*
ENV TERM=xterm

RUN cd /tmp \
    && wget https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.9.1%2B1/OpenJDK11U-jdk_x64_linux_hotspot_11.0.9.1_1.tar.gz -O java.tar.gz \
    && mkdir $HOME/java \
    && tar -xf java.tar.gz -C $HOME/java \
    && wget http://mirror.easyname.ch/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -O maven.tar.gz \
    && mkdir $HOME/maven \
    && tar -xf maven.tar.gz -C $HOME/maven \
    && wget https://github.com/openstreetmap/osmosis/releases/download/0.48.2/osmosis-0.48.2.tgz -O osmosis.tgz \
    && mkdir $HOME/osmosis \
    && tar -xf osmosis.tgz -C $HOME/osmosis \
    && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O miniconda.sh \
    && bash miniconda.sh -b -p $HOME/miniconda

ENV PATH=/root/miniconda/bin:/root/osmosis/bin:/root/java/jdk-11.0.9.1+1/bin:/root/maven/apache-maven-3.6.3/bin:$PATH
COPY environment.yml /srv/app/jsprit/

RUN echo $HOME $(ls $HOME)

RUN . "/root/miniconda/etc/profile.d/conda.sh" \
    && conda config --set always_yes yes --set changeps1 no \
    && conda update -q conda \
    && conda env create -f /srv/app/jsprit/environment.yml

RUN . "/root/miniconda/etc/profile.d/conda.sh" \
    && conda activate jsprit \
    && python3 --version && which python3 \
    && java -version && which java \
    && mvn -v && which mvn \
    && osmosis -v && which osmosis

ENV PATH=/root/miniconda/envs/jsprit/bin:$PATH

COPY ./java/src /srv/app/jsprit/java/src
COPY ./java/pom.xml /srv/app/jsprit/java/
RUN cd /srv/app/jsprit/java \
    && mvn package \
    && cd -

COPY entrypoint.sh prepare_perimeter.py prepare_osm.sh prepare_scenario.py /srv/app/jsprit/
COPY ./data/template_lyon.json /srv/app/data/
RUN chmod +x /srv/app/jsprit/entrypoint.sh

ENTRYPOINT [ "/srv/app/jsprit/entrypoint.sh" ]
